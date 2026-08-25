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

    private val TRAILING_PUNCTUATION = setOf('.', ',', '!', '?', ';', ':', ')', ']', '}', '"', '\'')

    fun trimTrailingPunctuation(rawUrl: String): Pair<String, Int> {
        var url = rawUrl
        var trimmed = 0
        while (url.isNotEmpty() && url.last() in TRAILING_PUNCTUATION) {
            val openParen = url.count { it == '(' }
            val closeParen = url.count { it == ')' }
            val openBracket = url.count { it == '[' }
            val closeBracket = url.count { it == ']' }

            if (url.last() == ')' && openParen >= closeParen) break
            if (url.last() == ']' && openBracket >= closeBracket) break

            url = url.dropLast(1)
            trimmed++
        }
        return Pair(url, trimmed)
    }

    @Composable
    fun createLinkedText(
        text: String,
        linkColor: Color = MaterialTheme.colorScheme.primary
    ): AnnotatedString {
        val matcher = URL_PATTERN.matcher(text)
        return buildAnnotatedString {
            append(text)
            while (matcher.find()) {
                val rawUrl = matcher.group()
                val (cleanUrl, trimmedCount) = trimTrailingPunctuation(rawUrl)
                if (cleanUrl.isBlank()) continue

                val start = matcher.start()
                val end = matcher.end() - trimmedCount

                if (start < end) {
                    addLink(
                        url = LinkAnnotation.Url(
                            url = cleanUrl,
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
    }

    fun extractUrls(text: String): List<String> {
        val matcher = URL_PATTERN.matcher(text)
        val urls = mutableListOf<String>()
        while (matcher.find()) {
            val (cleanUrl, _) = trimTrailingPunctuation(matcher.group())
            if (cleanUrl.isNotBlank()) {
                urls.add(cleanUrl)
            }
        }
        return urls
    }
}
