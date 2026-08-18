package com.arh.terminal.ui.session

import androidx.compose.runtime.Immutable
import com.arh.terminal.core.mcp.server.McpServerStats
import com.arh.terminal.core.relay.client.RelayStatus
import com.arh.terminal.data.profiles.ConnectionProfile
import com.arh.terminal.ui.conversation.AgentTurn
import com.arh.terminal.util.NetworkStatus
import com.arh.terminal.util.NetworkType

enum class ViewMode {
    AgentChat,
    RawTerminal,
    McpBridge
}

enum class TransportMode {
    DirectSsh,
    RelayWebSocket
}

@Immutable
data class PaneData(
    val paneId: String,
    val windowId: String,
    val title: String,
    val outputHistory: List<String> = emptyList(),
    val agentTurns: List<AgentTurn> = emptyList()
)

@Immutable
sealed interface ConnectionStatus {
    data object Disconnected : ConnectionStatus
    data class Connecting(val message: String) : ConnectionStatus
    data class Connected(val host: String) : ConnectionStatus
    data class Error(val message: String) : ConnectionStatus
}

@Immutable
data class SessionUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val transportMode: TransportMode = TransportMode.DirectSsh,
    val host: String = "127.0.0.1",
    val port: Int = 22,
    val username: String = "Administrator",
    val relayUrl: String = "wss://relay.arhsmoque.com/ws",
    val relaySecretKey: String = "e2ee-secret-passphrase",
    val relayStatus: RelayStatus = RelayStatus.Disconnected,
    val isAttached: Boolean = true,
    val activeSessionName: String = "arh-agent",
    val availableSessions: List<String> = listOf("arh-agent", "build-runner", "scratchpad"),
    val profiles: List<ConnectionProfile> = emptyList(),
    val selectedProfileId: String? = "default-win-box",
    val networkStatus: NetworkStatus = NetworkStatus(isConnected = true, type = NetworkType.Wifi),
    val mcpStats: McpServerStats = McpServerStats(),
    val viewMode: ViewMode = ViewMode.AgentChat,
    val panes: List<PaneData> = emptyList(),
    val selectedPaneId: String? = null,
    val pendingApprovalCommand: String? = null
)
