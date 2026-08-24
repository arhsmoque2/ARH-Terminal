package com.arh.terminal.ui.conversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * A chunk of a rendered message: either free-form prose or a fenced code block
 * (```lang\n...\n```). Mirrors how Claude/ChatGPT split a response into
 * alternating text/code segments so each code segment can get its own
 * copy-friendly, horizontally scrollable block.
 */
sealed interface MessageSegment {
    data class Prose(val text: String) : MessageSegment
    data class Code(val language: String?, val code: String) : MessageSegment
}

private val FENCE_REGEX = Regex("```([a-zA-Z0-9_+-]*)\\n?([\\s\\S]*?)```")

/** Splits [text] into prose/code segments on ``` fences. No fences => a single Prose segment. */
fun parseMessageSegments(text: String): List<MessageSegment> {
    if (text.isBlank()) return listOf(MessageSegment.Prose(text))

    val segments = mutableListOf<MessageSegment>()
    var cursor = 0
    for (match in FENCE_REGEX.findAll(text)) {
        if (match.range.first > cursor) {
            segments += MessageSegment.Prose(text.substring(cursor, match.range.first))
        }
        val lang = match.groupValues[1].trim().ifBlank { null }
        val code = match.groupValues[2].trimEnd('\n')
        segments += MessageSegment.Code(lang, code)
        cursor = match.range.last + 1
    }
    if (cursor < text.length) {
        segments += MessageSegment.Prose(text.substring(cursor))
    }
    return segments.ifEmpty { listOf(MessageSegment.Prose(text)) }
}

/**
 * Renders [text] as alternating prose and copyable code blocks, the way the
 * Claude and ChatGPT apps render fenced code inside an assistant reply.
 */
@Composable
fun MessageContentView(
    text: String,
    modifier: Modifier = Modifier,
    proseColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val segments = remember(text) { parseMessageSegments(text) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (segment in segments) {
            when (segment) {
                is MessageSegment.Prose -> {
                    if (segment.text.isNotBlank()) {
                        Text(
                            text = segment.text.trim('\n'),
                            style = MaterialTheme.typography.bodyMedium,
                            color = proseColor
                        )
                    }
                }
                is MessageSegment.Code -> CodeBlock(code = segment.code, language = segment.language)
            }
        }
    }
}

/**
 * A terminal/code-styled block with a language/label header and a 1-tap copy
 * button. Long lines scroll horizontally instead of wrapping, matching how
 * code blocks behave in the Claude and ChatGPT apps.
 */
@Composable
fun CodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    language: String? = null
) {
    val clipboard = LocalClipboardManager.current
    var justCopied by remember { mutableStateOf(false) }

    LaunchedEffect(justCopied) {
        if (justCopied) {
            delay(1500)
            justCopied = false
        }
    }

    Surface(
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language?.takeIf { it.isNotBlank() } ?: "code",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9E9E9E),
                    fontFamily = FontFamily.Monospace
                )
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        justCopied = true
                    }
                ) {
                    AnimatedContent(targetState = justCopied, label = "copy-icon") { copied ->
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = if (copied) "Copied" else "Copy code",
                            tint = if (copied) Color(0xFF69F0AE) else Color(0xFF9E9E9E)
                        )
                    }
                }
            }
            Surface(color = Color(0xFF161616)) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFE0E0E0),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
    }
}
