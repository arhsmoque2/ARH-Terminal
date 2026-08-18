package com.arh.terminal.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val tmuxFactory: TmuxClientFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var activeTmuxClient: TmuxClient? = null

    fun updateHost(host: String) = _uiState.update { it.copy(host = host) }
    fun updateUsername(user: String) = _uiState.update { it.copy(username = user) }
    fun updatePort(port: Int) = _uiState.update { it.copy(port = port) }

    fun connect(password: String = "") {
        val currentState = _uiState.value
        _uiState.update { it.copy(connectionStatus = ConnectionStatus.Connecting("Opening SSH to ${currentState.host}...")) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Establish SSH connection
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

                // 2. Attach psmux / tmux -CC control mode
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

                // 3. Listen to structured tmux/psmux control events
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
                _uiState.update { state ->
                    val updatedPanes = state.panes.map { p ->
                        if (p.paneId == paneId) {
                            val newHistory = (p.outputHistory + text).takeLast(200)
                            p.copy(outputHistory = newHistory)
                        } else p
                    }
                    state.copy(panes = updatedPanes)
                }
                detectApprovalPrompts(text)
            }
        }
    }

    private fun detectApprovalPrompts(text: String) {
        if (text.contains("[y/n]", ignoreCase = true) || text.contains("Approve command:", ignoreCase = true)) {
            _uiState.update { it.copy(pendingApprovalCommand = text.trim()) }
        }
    }

    fun sendCommand(command: String) {
        val client = activeTmuxClient ?: return
        val targetPane = _uiState.value.selectedPaneId ?: "%0"
        viewModelScope.launch(Dispatchers.IO) {
            client.sendCommand("send-keys -t $targetPane \"$command\" Enter")
        }
    }

    fun approvePrompt(approve: Boolean) {
        val reply = if (approve) "y" else "n"
        sendCommand(reply)
        _uiState.update { it.copy(pendingApprovalCommand = null) }
    }

    fun disconnect() {
        activeTmuxClient = null
        _uiState.update { it.copy(connectionStatus = ConnectionStatus.Disconnected, panes = emptyList()) }
    }
}
