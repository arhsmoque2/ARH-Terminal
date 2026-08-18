package com.arh.terminal.ui.session

import androidx.compose.runtime.Immutable
import com.arh.terminal.ui.conversation.AgentTurn

enum class ViewMode {
    AgentChat,
    RawTerminal
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
    val host: String = "127.0.0.1",
    val port: Int = 22,
    val username: String = "Administrator",
    val isAttached: Boolean = true,
    val activeSessionName: String = "arh-agent",
    val availableSessions: List<String> = listOf("arh-agent", "build-runner", "scratchpad"),
    val viewMode: ViewMode = ViewMode.AgentChat,
    val panes: List<PaneData> = emptyList(),
    val selectedPaneId: String? = null,
    val pendingApprovalCommand: String? = null
)
