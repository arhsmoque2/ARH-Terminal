package com.arh.terminal.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import java.util.regex.Pattern

object LinkifyHelper {
    private val URL_PATTERN: Pattern = Pattern.compile(
        "https?://[a-zA-Z0-9.-]+(?:\\.[a-zA-Z]{2,})+(?::\\d+)?(?:/[^\\s]*)?",
        Pattern.CASE_INSENSITIVE
    )

    @Composable
    fun createLinkedText(
        text: String,
        linkColor: Color = MaterialTheme.colorScheme.primary
    ): AnnotatedString {
        val matcher = URL_PATTERN.matcher(text)
        return buildAnnotatedString {
            append(text)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                val url = matcher.group()

                addLink(
                    url = LinkAnnotation.Url(
                        url = url,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    ),
                    start = start,
                    end = end
                )
            }
        }
    }

    fun extractUrls(text: String): List<String> {
        val matcher = URL_PATTERN.matcher(text)
        val urls = mutableListOf<String>()
        while (matcher.find()) {
            urls.add(matcher.group())
        }
        return urls
    }
}
