package com.arh.terminal.data.export

import com.arh.terminal.ui.conversation.AgentTurn
import com.pocketshell.core.agents.AgentKind
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionMarkdownExporterTest {

    @Test
    fun exportToMarkdown_formatsUserAndAssistantTurns() {
        val turns = listOf(
            AgentTurn.UserMessage(
                id = "1",
                timestamp = 1724500000000L,
                text = "List active git repos"
            ),
            AgentTurn.AssistantMessage(
                id = "2",
                timestamp = 1724500005000L,
                agent = AgentKind.ClaudeCode,
                text = "Here are your active repositories:\n```sh\ngit status\n```",
                thinking = "Analyzing user workspace request..."
            ),
            AgentTurn.ToolInvocation(
                id = "3",
                timestamp = 1724500010000L,
                toolName = "git_status",
                arguments = "{\"directory\": \"~/projects\"}",
                output = "On branch main\nnothing to commit",
                isPendingApproval = false
            )
        )

        val markdown = SessionMarkdownExporter.exportToMarkdown(
            sessionName = "work-session-1",
            host = "operator@100.64.0.1:22",
            turns = turns
        )

        assertTrue(markdown.contains("# ARH Terminal Session Transcript: work-session-1"))
        assertTrue(markdown.contains("`operator@100.64.0.1:22`"))
        assertTrue(markdown.contains("List active git repos"))
        assertTrue(markdown.contains("ClaudeCode"))
        assertTrue(markdown.contains("Reasoning Trace"))
        assertTrue(markdown.contains("Tool Call: `git_status`"))
        assertTrue(markdown.contains("{\"directory\": \"~/projects\"}"))
    }
}
