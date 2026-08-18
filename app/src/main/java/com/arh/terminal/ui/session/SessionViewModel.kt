package com.arh.terminal.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arh.terminal.core.mcp.server.McpServerEngine
import com.arh.terminal.data.profiles.ConnectionProfile
import com.arh.terminal.data.profiles.ProfileRepository
import com.arh.terminal.ui.conversation.AgentTurn
import com.arh.terminal.util.NetworkMonitor
import com.pocketshell.core.agents.ClaudeCodeParser
import com.pocketshell.core.agents.ConversationEvent
import com.pocketshell.core.agents.ConversationRole
import com.pocketshell.core.ssh.KnownHostsPolicy
import com.pocketshell.core.ssh.SshConnection
import com.pocketshell.core.ssh.SshKey
import com.pocketshell.core.ssh.SshSession
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
    private val tmuxFactory: TmuxClientFactory,
    private val profileRepository: ProfileRepository,
    private val networkMonitor: NetworkMonitor,
    private val mcpServerEngine: McpServerEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var activeSshSession: SshSession? = null
    private var activeTmuxClient: TmuxClient? = null
    private val claudeParser = ClaudeCodeParser()
    private var lastUsedKeyPem: String = ""

    init {
        viewModelScope.launch {
            profileRepository.profiles.collect { profilesList ->
                _uiState.update { it.copy(profiles = profilesList) }
            }
        }

        viewModelScope.launch {
            networkMonitor.status.collect { netStatus ->
                val wasDisconnected = !_uiState.value.networkStatus.isConnected
                _uiState.update { it.copy(networkStatus = netStatus) }

                if (wasDisconnected && netStatus.isConnected && activeSshSession == null && _uiState.value.connectionStatus is ConnectionStatus.Error) {
                    connect(lastUsedKeyPem)
                }
            }
        }

        viewModelScope.launch {
            mcpServerEngine.stats.collect { stats ->
                _uiState.update { it.copy(mcpStats = stats) }
            }
        }
    }

    fun toggleMcpServer(start: Boolean, port: Int = 8070, token: String = "ca48ffe8-cb63-45be-bfd5-1911e367fbcd") {
        if (start) {
            mcpServerEngine.start(port, token)
        } else {
            mcpServerEngine.stop()
        }
    }

    fun selectProfile(profile: ConnectionProfile) {
        _uiState.update {
            it.copy(
                selectedProfileId = profile.id,
                host = profile.host,
                port = profile.port,
                username = profile.username,
                activeSessionName = profile.defaultSession
            )
        }
    }

    fun saveCurrentAsProfile(name: String) {
        if (name.isBlank()) return
        val current = _uiState.value
        val newProfile = ConnectionProfile(
            name = name.trim(),
            host = current.host,
            port = current.port,
            username = current.username,
            defaultSession = current.activeSessionName
        )
        profileRepository.addProfile(newProfile)
    }

    fun updateHost(host: String) = _uiState.update { it.copy(host = host) }
    fun updateUsername(user: String) = _uiState.update { it.copy(username = user) }
    fun updatePort(port: Int) = _uiState.update { it.copy(port = port) }
    fun setViewMode(mode: ViewMode) = _uiState.update { it.copy(viewMode = mode) }

    fun connect(privateKeyPem: String = "") {
        lastUsedKeyPem = privateKeyPem
        val currentState = _uiState.value
        _uiState.update { it.copy(connectionStatus = ConnectionStatus.Connecting("Opening SSH to ${currentState.host}...")) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val keyContent = if (privateKeyPem.isNotBlank()) privateKeyPem else "DUMMY_KEY"
                val key = SshKey.Pem(keyContent)
                val sshResult = SshConnection.connect(
                    host = currentState.host,
                    port = currentState.port,
                    user = currentState.username,
                    key = key,
                    knownHosts = KnownHostsPolicy.AcceptAll
                )

                if (sshResult.isFailure) {
                    val err = sshResult.exceptionOrNull()?.message ?: "SSH connection failed"
                    _uiState.update { it.copy(connectionStatus = ConnectionStatus.Error(err)) }
                    return@launch
                }

                val session = sshResult.getOrThrow()
                activeSshSession = session

                _uiState.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.Connected(host = currentState.host),
                        isAttached = true
                    )
                }

                attachTmuxClient(session)

            } catch (e: Exception) {
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Error(e.message ?: "Unknown error")) }
            }
        }
    }

    private fun attachTmuxClient(session: SshSession) {
        val tmuxClient = tmuxFactory.create(session)
        activeTmuxClient = tmuxClient

        _uiState.update { it.copy(isAttached = true) }

        viewModelScope.launch(Dispatchers.IO) {
            tmuxClient.connect()
            tmuxClient.events.collect { event ->
                handleControlEvent(event)
            }
        }
    }

    fun toggleAttach(attach: Boolean) {
        val session = activeSshSession ?: return
        if (attach) {
            attachTmuxClient(session)
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                activeTmuxClient?.sendCommand("detach-client")
                activeTmuxClient = null
                _uiState.update { it.copy(isAttached = false) }
            }
        }
    }

    fun switchSession(sessionName: String) {
        if (sessionName == _uiState.value.activeSessionName) return
        val session = activeSshSession ?: return
        viewModelScope.launch(Dispatchers.IO) {
            activeTmuxClient?.sendCommand("detach-client")
            activeTmuxClient = null
            _uiState.update { it.copy(activeSessionName = sessionName, panes = emptyList(), isAttached = true) }
            attachTmuxClient(session)
        }
    }

    fun createNewSession(sessionName: String) {
        if (sessionName.isBlank()) return
        _uiState.update {
            val sessions = (it.availableSessions + sessionName.trim()).distinct()
            it.copy(availableSessions = sessions)
        }
        switchSession(sessionName.trim())
    }

    private fun handleControlEvent(event: ControlEvent) {
        when (event) {
            is ControlEvent.SessionChanged -> {
                _uiState.update { it.copy(activeSessionName = event.name) }
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
            client.outputFor(paneId).collect { outputEvent ->
                val text = String(outputEvent.data, StandardCharsets.UTF_8)
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
                val time = evt.atMillis ?: System.currentTimeMillis()
                when (evt) {
                    is ConversationEvent.Message -> {
                        if (evt.role == ConversationRole.User) {
                            turns.add(AgentTurn.UserMessage(evt.id, time, evt.text))
                        } else {
                            turns.add(AgentTurn.AssistantMessage(evt.id, time, evt.agent, evt.text))
                        }
                    }
                    is ConversationEvent.ToolCall -> {
                        turns.add(AgentTurn.ToolInvocation(evt.id, time, evt.name, evt.input))
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

    fun sendRawKey(rawSequence: String) {
        val client = activeTmuxClient ?: return
        val targetPane = _uiState.value.selectedPaneId ?: "%0"
        viewModelScope.launch(Dispatchers.IO) {
            client.sendCommand("send-keys -t $targetPane \"$rawSequence\"")
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
        activeSshSession = null
        _uiState.update {
            it.copy(
                connectionStatus = ConnectionStatus.Disconnected,
                panes = emptyList(),
                isAttached = false
            )
        }
    }
}
