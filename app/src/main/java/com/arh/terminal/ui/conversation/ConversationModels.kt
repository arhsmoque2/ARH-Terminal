package com.arh.terminal.ui.conversation

import androidx.compose.runtime.Immutable
import com.pocketshell.core.agents.AgentKind
import com.pocketshell.core.agents.ConversationRole

@Immutable
sealed interface AgentTurn {
    val id: String
    val timestamp: Long

    @Immutable
    data class UserMessage(
        override val id: String,
        override val timestamp: Long,
        val text: String
    ) : AgentTurn

    @Immutable
    data class AssistantMessage(
        override val id: String,
        override val timestamp: Long,
        val agent: AgentKind,
        val text: String,
        val thinking: String? = null
    ) : AgentTurn

    @Immutable
    data class ToolInvocation(
        override val id: String,
        override val timestamp: Long,
        val toolName: String,
        val arguments: String,
        val output: String? = null,
        val isPendingApproval: Boolean = false
    ) : AgentTurn
}
