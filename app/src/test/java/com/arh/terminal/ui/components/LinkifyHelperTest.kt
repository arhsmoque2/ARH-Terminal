package com.arh.terminal.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkifyHelperTest {

    @Test
    fun extractUrls_findsHttpAndHttpsUrls() {
        val text = "Check out https://github.com/arhsmoque2/ARH-Terminal and http://example.com:8080/docs for details."
        val urls = LinkifyHelper.extractUrls(text)

        assertEquals(2, urls.size)
        assertEquals("https://github.com/arhsmoque2/ARH-Terminal", urls[0])
        assertEquals("http://example.com:8080/docs", urls[1])
    }

    @Test
    fun extractUrls_returnsEmptyForPlainProse() {
        val text = "This is plain prose with no links."
        val urls = LinkifyHelper.extractUrls(text)

        assertTrue(urls.isEmpty())
    }
}
