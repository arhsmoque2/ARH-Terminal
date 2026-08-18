package com.arh.terminal.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arh.terminal.ui.conversation.AgentTurn
import com.pocketshell.core.agents.AgentKind
import com.pocketshell.core.agents.ClaudeCodeParser
import com.pocketshell.core.agents.ConversationEvent
import com.pocketshell.core.agents.ConversationRole
import com.pocketshell.core.ssh.SshConnection
import com.pocketshell.core.tmux.TmuxClient
import com.pocketshell.core.tmux.TmuxClientFactory
import com.pocketshell.core.tmux.protocol.ControlEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val tmuxFactory: TmuxClientFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var activeTmuxClient: TmuxClient? = null
    private val claudeParser = ClaudeCodeParser()

    fun updateHost(host: String) = _uiState.update { it.copy(host = host) }
    fun updateUsername(user: String) = _uiState.update { it.copy(username = user) }
    fun updatePort(port: Int) = _uiState.update { it.copy(port = port) }
    fun setViewMode(mode: ViewMode) = _uiState.update { it.copy(viewMode = mode) }

    fun connect(password: String = "") {
        val currentState = _uiState.value
        _uiState.update { it.copy(connectionStatus = ConnectionStatus.Connecting("Opening SSH to ${currentState.host}...")) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sshResult = SshConnection.connect(
                    host = currentState.host,
                    port = currentState.port,
                    username = currentState.username,
                    password = password
                )

                if (sshResult.isFailure) {
                    val err = sshResult.exceptionOrNull()?.message ?: "SSH connection failed"
                    _uiState.update { it.copy(connectionStatus = ConnectionStatus.Error(err)) }
                    return@launch
                }

                val session = sshResult.getOrThrow()
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Connecting("Attaching psmux/tmux -CC session...")) }

                val tmuxClient = tmuxFactory.create(session)
                activeTmuxClient = tmuxClient
                tmuxClient.connect()

                _uiState.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.Connected(
                            sessionName = currentState.activeSessionName,
                            host = currentState.host
                        )
                    )
                }

                launch {
                    tmuxClient.events.collect { event ->
                        handleControlEvent(event)
                    }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Error(e.message ?: "Unknown error")) }
            }
        }
    }

    private fun handleControlEvent(event: ControlEvent) {
        when (event) {
            is ControlEvent.SessionChanged -> {
                _uiState.update { it.copy(activeSessionName = event.sessionName) }
            }
            is ControlEvent.WindowAdd -> {
                val pane = PaneData(
                    paneId = "%0",
                    windowId = event.windowId,
                    title = "Window ${event.windowId}"
                )
                _uiState.update { state ->
                    val existing = state.panes.filterNot { it.windowId == event.windowId }
                    state.copy(panes = existing + pane, selectedPaneId = pane.paneId)
                }
                listenToPaneOutput("%0")
            }
            is ControlEvent.WindowClose -> {
                _uiState.update { state ->
                    val filtered = state.panes.filterNot { it.windowId == event.windowId }
                    state.copy(panes = filtered, selectedPaneId = filtered.firstOrNull()?.paneId)
                }
            }
            else -> Unit
        }
    }

    private fun listenToPaneOutput(paneId: String) {
        val client = activeTmuxClient ?: return
        viewModelScope.launch(Dispatchers.IO) {
            client.outputFor(paneId).collect { bytes ->
                val text = String(bytes, StandardCharsets.UTF_8)
                val turns = parseOutputTurns(text)

                _uiState.update { state ->
                    val updatedPanes = state.panes.map { p ->
                        if (p.paneId == paneId) {
                            val newHistory = (p.outputHistory + text).takeLast(200)
                            val newTurns = p.agentTurns + turns
                            p.copy(outputHistory = newHistory, agentTurns = newTurns)
                        } else p
                    }
                    state.copy(panes = updatedPanes)
                }
                detectApprovalPrompts(text)
            }
        }
    }

    private fun parseOutputTurns(text: String): List<AgentTurn> {
        val turns = mutableListOf<AgentTurn>()
        val lines = text.lines()
        for (line in lines) {
            val events = claudeParser.parseLine(line)
            for (evt in events) {
                when (evt) {
                    is ConversationEvent.Message -> {
                        if (evt.role == ConversationRole.User) {
                            turns.add(AgentTurn.UserMessage(evt.id, evt.timestampMillis, evt.text))
                        } else {
                            turns.add(AgentTurn.AssistantMessage(evt.id, evt.timestampMillis, evt.agent, evt.text))
                        }
                    }
                    is ConversationEvent.ToolCall -> {
                        turns.add(AgentTurn.ToolInvocation(evt.id, evt.timestampMillis, evt.toolName, evt.arguments))
                    }
                    is ConversationEvent.ToolResult -> {
                        val lastIdx = turns.indexOfLast { it is AgentTurn.ToolInvocation }
                        if (lastIdx >= 0) {
                            val prev = turns[lastIdx] as AgentTurn.ToolInvocation
                            turns[lastIdx] = prev.copy(output = evt.output)
                        }
                    }
                    else -> Unit
                }
            }
        }
        return turns
    }

    private fun detectApprovalPrompts(text: String) {
        if (text.contains("[y/n]", ignoreCase = true) || text.contains("Approve command:", ignoreCase = true)) {
            _uiState.update { it.copy(pendingApprovalCommand = text.trim()) }
        }
    }

    fun sendPrompt(prompt: String) {
        if (prompt.isBlank()) return
        val userTurn = AgentTurn.UserMessage(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            text = prompt
        )
        val targetPane = _uiState.value.selectedPaneId ?: "%0"
        _uiState.update { state ->
            val updatedPanes = state.panes.map { p ->
                if (p.paneId == targetPane) p.copy(agentTurns = p.agentTurns + userTurn) else p
            }
            state.copy(panes = updatedPanes)
        }

        val client = activeTmuxClient ?: return
        viewModelScope.launch(Dispatchers.IO) {
            client.sendCommand("send-keys -t $targetPane \"$prompt\" Enter")
        }
    }

    fun approvePrompt(approve: Boolean) {
        val reply = if (approve) "y" else "n"
        val client = activeTmuxClient ?: return
        val targetPane = _uiState.value.selectedPaneId ?: "%0"
        viewModelScope.launch(Dispatchers.IO) {
            client.sendCommand("send-keys -t $targetPane \"$reply\" Enter")
        }
        _uiState.update { it.copy(pendingApprovalCommand = null) }
    }

    fun disconnect() {
        activeTmuxClient = null
        _uiState.update { it.copy(connectionStatus = ConnectionStatus.Disconnected, panes = emptyList()) }
    }
}
