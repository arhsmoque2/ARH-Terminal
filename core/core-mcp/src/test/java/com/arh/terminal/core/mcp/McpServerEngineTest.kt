package com.arh.terminal.core.mcp

import com.arh.terminal.core.mcp.bridge.DefaultAccessibilityBridge
import com.arh.terminal.core.mcp.protocol.McpToolDefinition
import com.arh.terminal.core.mcp.server.McpServerEngine
import com.arh.terminal.core.mcp.tools.AndroidToolRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class McpServerEngineTest {

    private val bridge = DefaultAccessibilityBridge()
    private val registry = AndroidToolRegistry(bridge)
    private val engine = McpServerEngine(registry)

    @Test
    fun toolRegistryContainsCoreAndroidTools() {
        val tools = registry.listTools()
        assertTrue(tools.isNotEmpty())
        assertNotNull(tools.find { it.name == "android_get_screen_state" })
        assertNotNull(tools.find { it.name == "android_tap" })
        assertNotNull(tools.find { it.name == "android_set_clipboard" })
    }

    @Test
    fun executeGetScreenStateReturnsOk() {
        val res = registry.executeTool("android_get_screen_state", JSONObject())
        assertFalse(res.isError)
        assertEquals(1, res.content.size)
        assertTrue(res.content[0].text?.contains("com.arh.terminal") == true)
    }

    @Test
    fun executeClipboardBridgeSetsAndGets() {
        registry.executeTool("android_set_clipboard", JSONObject().put("text", "hello-from-agent"))
        val getRes = registry.executeTool("android_get_clipboard", JSONObject())
        assertEquals("hello-from-agent", getRes.content[0].text)
    }
}
