package com.arh.terminal.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arh.terminal.core.mcp.server.McpServerEngine
import com.arh.terminal.core.relay.client.RelayStatus
import com.arh.terminal.core.relay.client.RelayWebSocketClient
import com.arh.terminal.data.audit.AgentAuditJournal
import com.arh.terminal.data.audit.ConsentTier
import com.arh.terminal.data.profiles.ConnectionProfile
import com.arh.terminal.data.profiles.ProfileRepository
import com.arh.terminal.ui.conversation.AgentTurn
import com.arh.terminal.util.NetworkMonitor
import com.pocketshell.core.agents.AgentKind
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
import com.arh.terminal.core.mcp.tools.ConsentGate
import com.arh.terminal.core.mcp.tools.ToolConsentTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val tmuxFactory: TmuxClientFactory,
    private val profileRepository: ProfileRepository,
    private val networkMonitor: NetworkMonitor,
    private val mcpServerEngine: McpServerEngine,
    private val relayClient: RelayWebSocketClient,
    private val auditJournal: AgentAuditJournal
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var activeSshSession: SshSession? = null
    private var activeTmuxClient: TmuxClient? = null
    private val claudeParser = ClaudeCodeParser()
    private var lastUsedKeyPem: String = ""
    private var pendingConsentDeferred: CompletableDeferred<Boolean>? = null

    init {
        mcpServerEngine.toolRegistry.consentGate = ConsentGate { name, args, tier ->
            requestOperatorConsent(name, args, tier)
        }
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

        viewModelScope.launch {
            auditJournal.records.collect { recordsList ->
                _uiState.update { it.copy(auditRecords = recordsList) }
            }
        }

        viewModelScope.launch {
            relayClient.status.collect { rStatus ->
                _uiState.update { it.copy(relayStatus = rStatus) }
                if (rStatus is RelayStatus.Connected) {
                    _uiState.update {
                        it.copy(connectionStatus = ConnectionStatus.Connected(rStatus.url), isAttached = true)
                    }
                }
            }
        }
    }

    fun setTransportMode(mode: TransportMode) { _uiState.update { it.copy(transportMode = mode) } }
    fun setHost(host: String) { _uiState.update { it.copy(host = host) } }
    fun updateHost(host: String) { setHost(host) }
    fun setPort(port: Int) { _uiState.update { it.copy(port = port) } }
    fun setUsername(user: String) { _uiState.update { it.copy(username = user) } }
    fun updateUsername(user: String) { setUsername(user) }
    fun setRelayUrl(url: String) { _uiState.update { it.copy(relayUrl = url) } }
    fun setRelaySecretKey(key: String) { _uiState.update { it.copy(relaySecretKey = key) } }
    fun setViewMode(mode: ViewMode) { _uiState.update { it.copy(viewMode = mode) } }

    fun toggleJoypad() {
        _uiState.update { it.copy(showJoypad = !it.showJoypad) }
    }

    fun toggleTmuxPicker(show: Boolean) {
        _uiState.update { it.copy(showTmuxPicker = show) }
    }

    fun toggleMacrosModal(show: Boolean) {
        _uiState.update { it.copy(showMacrosModal = show) }
    }

    fun toggleMcpServer() {
        if (_uiState.value.mcpStats.isRunning) {
            mcpServerEngine.stop()
            auditJournal.recordAction("MCP Server", "server_stop", "Stopped on-device HTTP daemon", ConsentTier.READ_ONLY)
        } else {
            mcpServerEngine.start()
            auditJournal.recordAction("MCP Server", "server_start", "Started daemon on port 8070", ConsentTier.READ_ONLY)
        }
    }

    fun selectProfile(profileId: String) {
        val profile = _uiState.value.profiles.find { it.id == profileId } ?: return
        _uiState.update {
            it.copy(
                selectedProfileId = profileId,
                host = profile.host,
                port = profile.port,
                username = profile.username,
                activeSessionName = profile.defaultSession
            )
        }
    }

    fun saveCurrentAsProfile(name: String) {
        val newProfile = ConnectionProfile(
            name = name,
            host = _uiState.value.host,
            port = _uiState.value.port,
            username = _uiState.value.username,
            defaultSession = _uiState.value.activeSessionName
        )
        profileRepository.addProfile(newProfile)
    }

    fun connect(privateKeyPem: String = "") {
        lastUsedKeyPem = privateKeyPem

        if (_uiState.value.transportMode == TransportMode.RelayWebSocket) {
            connectRelay()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(connectionStatus = ConnectionStatus.Connecting("Connecting to ${_uiState.value.host}...")) }

            val key = if (privateKeyPem.isNotBlank()) {
                SshKey.Pem(privateKeyPem)
            } else {
                SshKey.Path(File("/dev/null"))
            }

            val result = SshConnection.connect(
                host = _uiState.value.host,
                port = _uiState.value.port,
                user = _uiState.value.username,
                key = key,
                knownHosts = KnownHostsPolicy.AcceptAll
            )

            result.onSuccess { session ->
                activeSshSession = session
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Connected(_uiState.value.host)) }
                auditJournal.recordAction("SSH Transport", "session_open", "Connected to ${_uiState.value.host}:${_uiState.value.port}", ConsentTier.READ_ONLY)
                
                _uiState.update { it.copy(showTmuxPicker = true) }
                attachTmux(_uiState.value.activeSessionName)
            }.onFailure { error ->
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Error(error.message ?: "Connection failed")) }
            }
        }
    }

    fun disconnect() {
        detachTmux()
        activeSshSession?.close()
        activeSshSession = null
        relayClient.disconnect()
        _uiState.update { it.copy(connectionStatus = ConnectionStatus.Disconnected, isAttached = false) }
        auditJournal.recordAction("Session", "disconnect", "Disconnected from host", ConsentTier.READ_ONLY)
    }

    private fun connectRelay() {
        relayClient.connect(_uiState.value.relayUrl, _uiState.value.relaySecretKey)
        auditJournal.recordAction("E2EE Relay", "relay_connect", "Connected to ${_uiState.value.relayUrl}", ConsentTier.READ_ONLY)
    }

    fun toggleAttach() {
        if (_uiState.value.isAttached) {
            detachTmux()
        } else {
            attachTmux(_uiState.value.activeSessionName)
        }
    }

    fun switchSession(sessionName: String) {
        attachTmux(sessionName)
    }

    fun createNewSession(sessionName: String) {
        attachTmux(sessionName)
    }

    fun attachTmux(sessionName: String) {
        val ssh = activeSshSession ?: return
        _uiState.update { it.copy(activeSessionName = sessionName, isAttached = true, showTmuxPicker = false) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = tmuxFactory.create(ssh, sessionName)
                activeTmuxClient = client
                client.connect()

                client.events.collect { event ->
                    handleTmuxEvent(event)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Error("psmux attach failed: ${e.message}")) }
            }
        }
    }

    fun detachTmux() {
        activeTmuxClient?.close()
        activeTmuxClient = null
        _uiState.update { it.copy(isAttached = false) }
        auditJournal.recordAction("psmux", "detach", "Detached from session ${_uiState.value.activeSessionName}", ConsentTier.READ_ONLY)
    }

    fun selectPane(paneId: String) {
        _uiState.update { it.copy(selectedPaneId = paneId) }
    }

    private suspend fun requestOperatorConsent(toolName: String, args: org.json.JSONObject, tier: ToolConsentTier): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        pendingConsentDeferred = deferred
        val prompt = "MCP [${tier.name}] $toolName: $args"
        _uiState.update { it.copy(pendingApprovalCommand = prompt) }

        val approved = try {
            deferred.await()
        } finally {
            pendingConsentDeferred = null
            _uiState.update { it.copy(pendingApprovalCommand = null) }
        }
        return approved
    }

    fun approvePrompt() {
        approveCommand()
    }

    fun approveCommand() {
        val pendingConsent = pendingConsentDeferred
        if (pendingConsent != null) {
            val cmd = _uiState.value.pendingApprovalCommand ?: "MCP Action"
            auditJournal.recordAction("Consent HUD", "approve_mcp_action", cmd, ConsentTier.MUTATIVE, approved = true)
            pendingConsent.complete(true)
            _uiState.update { it.copy(pendingApprovalCommand = null) }
            return
        }

        val cmd = _uiState.value.pendingApprovalCommand
        auditJournal.recordAction("Approval HUD", "approve_command", cmd ?: "unknown", ConsentTier.MUTATIVE, approved = true)
        sendCommand("y\n")
        _uiState.update { it.copy(pendingApprovalCommand = null) }
    }

    fun rejectCommand() {
        val pendingConsent = pendingConsentDeferred
        if (pendingConsent != null) {
            val cmd = _uiState.value.pendingApprovalCommand ?: "MCP Action"
            auditJournal.recordAction("Consent HUD", "reject_mcp_action", cmd, ConsentTier.MUTATIVE, approved = false)
            pendingConsent.complete(false)
            _uiState.update { it.copy(pendingApprovalCommand = null) }
            return
        }

        val cmd = _uiState.value.pendingApprovalCommand
        auditJournal.recordAction("Approval HUD", "reject_command", cmd ?: "unknown", ConsentTier.MUTATIVE, approved = false)
        sendCommand("n\n")
        _uiState.update { it.copy(pendingApprovalCommand = null) }
    }

    fun sendRawKey(keySeq: String) {
        sendCommand(keySeq)
    }

    fun sendCommand(text: String) {
        val target = _uiState.value.selectedPaneId ?: return
        val client = activeTmuxClient ?: return
        viewModelScope.launch(Dispatchers.IO) {
            client.sendKeysViaExec("send-keys -t $target \"$text\"")
        }
    }

    fun sendPrompt(promptText: String) {
        val target = _uiState.value.selectedPaneId ?: _uiState.value.panes.firstOrNull()?.paneId ?: return
        if (promptText.isBlank()) return

        auditJournal.recordAction("Agent Prompt", "send_prompt", promptText.take(60), ConsentTier.MUTATIVE)

        if (_uiState.value.transportMode == TransportMode.RelayWebSocket) {
            relayClient.send(promptText)
            return
        }

        val client = activeTmuxClient ?: return
        viewModelScope.launch(Dispatchers.IO) {
            client.sendKeysViaExec("send-keys -t $target \"$promptText\" Enter")
        }
    }

    fun setPendingSharedPrompt(text: String) {
        handleSharedPrompt(text)
    }

    fun handleSharedPrompt(text: String) {
        _uiState.update { it.copy(pendingSharedPrompt = text) }
    }

    fun clearSharedPrompt() {
        _uiState.update { it.copy(pendingSharedPrompt = null) }
    }

    private fun handleTmuxEvent(event: ControlEvent) {
        when (event) {
            is ControlEvent.Output -> {
                val pId = event.paneId
                val rawText = String(event.data, StandardCharsets.UTF_8)

                if (rawText.contains("[y/n]") || rawText.contains("Do you want to proceed?")) {
                    _uiState.update { it.copy(pendingApprovalCommand = rawText.trim()) }
                }

                _uiState.update { state ->
                    val existingPane = state.panes.find { it.paneId == pId }
                    val currentTurns = existingPane?.agentTurns ?: emptyList()
                    val newTurns = parseOutputTurns(rawText, currentTurns)

                    val updatedPanes = if (existingPane != null) {
                        state.panes.map { p ->
                            if (p.paneId == pId) {
                                p.copy(
                                    outputHistory = (p.outputHistory + rawText).takeLast(100),
                                    agentTurns = newTurns
                                )
                            } else p
                        }
                    } else {
                        state.panes + PaneData(
                            paneId = pId,
                            windowId = "@0",
                            title = "Agent Pane ($pId)",
                            outputHistory = listOf(rawText),
                            agentTurns = newTurns
                        )
                    }

                    state.copy(
                        panes = updatedPanes,
                        selectedPaneId = state.selectedPaneId ?: pId
                    )
                }
            }
            is ControlEvent.SessionChanged -> {
                _uiState.update { it.copy(activeSessionName = event.name) }
            }
            else -> Unit
        }
    }

    private fun parseOutputTurns(chunk: String, currentTurns: List<AgentTurn>): List<AgentTurn> {
        val turns = currentTurns.toMutableList()
        val events = claudeParser.parseLine(chunk)

        for (e in events) {
            when (e) {
                is ConversationEvent.Message -> {
                    if (e.role == ConversationRole.User) {
                        turns.add(
                            AgentTurn.UserMessage(
                                id = e.id,
                                timestamp = e.atMillis ?: System.currentTimeMillis(),
                                text = e.text
                            )
                        )
                    } else {
                        turns.add(
                            AgentTurn.AssistantMessage(
                                id = e.id,
                                timestamp = e.atMillis ?: System.currentTimeMillis(),
                                agent = e.agent,
                                text = e.text
                            )
                        )
                    }
                }
                is ConversationEvent.ToolCall -> {
                    turns.add(
                        AgentTurn.ToolInvocation(
                            id = e.id,
                            timestamp = e.atMillis ?: System.currentTimeMillis(),
                            toolName = e.name,
                            arguments = e.input
                        )
                    )
                }
                is ConversationEvent.ToolResult -> {
                    val last = turns.lastOrNull()
                    if (last is AgentTurn.ToolInvocation && last.id == e.toolCallId) {
                        turns[turns.lastIndex] = last.copy(output = e.output)
                    }
                }
                else -> Unit
            }
        }
        return turns.takeLast(100)
    }
}
