package com.arh.terminal.data.export

import android.content.Context
import android.content.Intent
import com.arh.terminal.ui.conversation.AgentTurn
import com.arh.terminal.util.SecretRedaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SessionMarkdownExporter {

    fun exportToMarkdown(
        sessionName: String,
        host: String,
        turns: List<AgentTurn>
    ): String = buildString {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        appendLine("# ARH Terminal Session Transcript: $sessionName")
        appendLine()
        appendLine("- **Host / Target**: `$host`")
        appendLine("- **Exported At**: `${dateFormat.format(Date())}`")
        appendLine("- **Total Turns**: ${turns.size}")
        appendLine()
        appendLine("---")
        appendLine()

        turns.forEachIndexed { index, turn ->
            val time = dateFormat.format(Date(turn.timestamp))
            when (turn) {
                is AgentTurn.UserMessage -> {
                    appendLine("### Turn ${index + 1} — Operator ($time)")
                    appendLine()
                    appendLine(SecretRedaction.redact(turn.text))
                    appendLine()
                }
                is AgentTurn.AssistantMessage -> {
                    appendLine("### Turn ${index + 1} — Agent (${turn.agent.name}) ($time)")
                    appendLine()
                    if (!turn.thinking.isNullOrBlank()) {
                        appendLine("> [!NOTE]")
                        appendLine("> **Reasoning Trace**:")
                        turn.thinking.lines().forEach { line ->
                            appendLine("> ${SecretRedaction.redact(line)}")
                        }
                        appendLine()
                    }
                    appendLine(SecretRedaction.redact(turn.text))
                    appendLine()
                }
                is AgentTurn.ToolInvocation -> {
                    appendLine("### Turn ${index + 1} — Tool Call: `${turn.toolName}` ($time)")
                    appendLine()
                    appendLine("**Arguments:**")
                    appendLine("```json")
                    appendLine(SecretRedaction.redact(turn.arguments))
                    appendLine("```")
                    appendLine()
                    if (turn.output != null) {
                        appendLine("**Output:**")
                        appendLine("```")
                        appendLine(SecretRedaction.redact(turn.output))
                        appendLine("```")
                        appendLine()
                    }
                    if (turn.isPendingApproval) {
                        appendLine("*Status: Pending Operator Approval*")
                        appendLine()
                    }
                }
            }
            appendLine("---")
            appendLine()
        }
    }

    fun shareSession(
        context: Context,
        sessionName: String,
        host: String,
        turns: List<AgentTurn>
    ) {
        val markdown = exportToMarkdown(sessionName, host, turns)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, markdown)
            putExtra(Intent.EXTRA_TITLE, "Session-$sessionName.md")
            type = "text/markdown"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export Session Transcript")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
