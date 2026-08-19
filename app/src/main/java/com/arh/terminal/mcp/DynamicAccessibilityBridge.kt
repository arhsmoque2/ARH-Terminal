package com.arh.terminal.mcp

import com.arh.terminal.core.mcp.bridge.AccessibilityBridge
import com.arh.terminal.core.mcp.bridge.DefaultAccessibilityBridge
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dynamic delegation bridge connecting MCP Tool Registry to [McpAccessibilityService]
 * when the service is enabled in Android Settings, falling back to [DefaultAccessibilityBridge]
 * when not running or detached.
 */
@Singleton
class DynamicAccessibilityBridge @Inject constructor() : AccessibilityBridge {

    private val fallback = DefaultAccessibilityBridge()

    val isServiceConnected: Boolean
        get() = McpAccessibilityService.instance?.get() != null

    private val delegate: AccessibilityBridge
        get() = McpAccessibilityService.instance?.get() ?: fallback

    override fun getScreenState(): JSONObject = delegate.getScreenState()

    override fun findNodes(query: String): JSONArray = delegate.findNodes(query)

    override fun getNodeDetails(nodeId: String): JSONObject? = delegate.getNodeDetails(nodeId)

    override fun performTap(x: Int, y: Int): Boolean = delegate.performTap(x, y)

    override fun performSwipe(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Long): Boolean =
        delegate.performSwipe(startX, startY, endX, endY, durationMs)

    override fun performKey(keyCode: Int): Boolean = delegate.performKey(keyCode)

    override fun typeText(text: String): Boolean = delegate.typeText(text)

    override fun getClipboard(): String? = delegate.getClipboard()

    override fun setClipboard(text: String): Boolean = delegate.setClipboard(text)

    override fun openApp(packageName: String): Boolean = delegate.openApp(packageName)

    override fun getDeviceLogs(): String = delegate.getDeviceLogs()

    override fun getNotifications(): JSONArray = delegate.getNotifications()
}
