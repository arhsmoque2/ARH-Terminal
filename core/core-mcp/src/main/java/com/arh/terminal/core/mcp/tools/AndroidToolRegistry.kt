package com.arh.terminal.core.mcp.tools

import com.arh.terminal.core.mcp.bridge.AccessibilityBridge
import com.arh.terminal.core.mcp.protocol.McpCallResult
import com.arh.terminal.core.mcp.protocol.McpContent
import com.arh.terminal.core.mcp.protocol.McpToolDefinition
import org.json.JSONObject

class AndroidToolRegistry(
    private val bridge: AccessibilityBridge
) {
    private val tools = mutableMapOf<String, Pair<McpToolDefinition, (JSONObject) -> McpCallResult>>()

    init {
        registerScreenTools()
        registerGestureTools()
        registerKeyboardTools()
        registerSystemTools()
    }

    fun listTools(): List<McpToolDefinition> = tools.values.map { it.first }

    fun executeTool(name: String, args: JSONObject): McpCallResult {
        val entry = tools[name] ?: return McpCallResult(
            isError = true,
            content = listOf(McpContent(type = "text", text = "Tool '$name' not found."))
        )
        return try {
            entry.second(args)
        } catch (e: Exception) {
            McpCallResult(isError = true, content = listOf(McpContent(type = "text", text = "Execution error: ${e.message}")))
        }
    }

    private fun register(name: String, description: String, schema: JSONObject = JSONObject(), handler: (JSONObject) -> McpCallResult) {
        val def = McpToolDefinition(name = name, description = description, inputSchema = schema)
        tools[name] = def to handler
    }

    private fun registerScreenTools() {
        register("android_get_screen_state", "Captures the complete accessibility UI tree and screen attributes.") {
            val state = bridge.getScreenState()
            McpCallResult(content = listOf(McpContent(text = state.toString(2))))
        }
        register("android_find_nodes", "Search UI nodes by text or resource ID.") { args ->
            val query = args.optString("query", "")
            val results = bridge.findNodes(query)
            McpCallResult(content = listOf(McpContent(text = results.toString(2))))
        }
        register("android_get_node_details", "Get detailed bounds and flags for a specific node ID.") { args ->
            val nodeId = args.optString("node_id", "")
            val details = bridge.getNodeDetails(nodeId) ?: run {
                val matches = bridge.findNodes(nodeId)
                if (matches.length() > 0) matches.getJSONObject(0) else null
            }
            if (details != null) {
                McpCallResult(content = listOf(McpContent(text = details.toString(2))))
            } else {
                val notFound = JSONObject().apply {
                    put("node_id", nodeId)
                    put("found", false)
                    put("status", "node_not_found")
                }
                McpCallResult(content = listOf(McpContent(text = notFound.toString(2))))
            }
        }
        register("android_wait_for_node", "Wait until element matching query appears on screen.") { args ->
            val query = args.optString("query", "")
            val found = bridge.findNodes(query)
            if (found.length() > 0) {
                McpCallResult(content = listOf(McpContent(text = "Node '$query' found on screen (${found.length()} matches).")))
            } else {
                McpCallResult(content = listOf(McpContent(text = "Node '$query' not yet visible on screen.")))
            }
        }
        register("android_wait_for_idle", "Wait for animations and UI settle.") {
            val state = bridge.getScreenState()
            val nodeCount = state.optInt("nodesCount", 0)
            McpCallResult(content = listOf(McpContent(text = "UI settled with $nodeCount active nodes.")))
        }
    }

    private fun registerGestureTools() {
        register("android_tap", "Tap on screen coordinates (x, y).") { args ->
            val x = args.optInt("x", 0)
            val y = args.optInt("y", 0)
            val success = bridge.performTap(x, y)
            McpCallResult(content = listOf(McpContent(text = if (success) "Tapped at ($x, $y)" else "Tap failed")))
        }
        register("android_swipe", "Perform drag/swipe gesture from (startX, startY) to (endX, endY).") { args ->
            val sx = args.optInt("startX", 0)
            val sy = args.optInt("startY", 0)
            val ex = args.optInt("endX", 0)
            val ey = args.optInt("endY", 0)
            val dur = args.optLong("durationMs", 300L)
            val ok = bridge.performSwipe(sx, sy, ex, ey, dur)
            McpCallResult(content = listOf(McpContent(text = if (ok) "Swiped successfully" else "Swipe failed")))
        }
        register("android_press_back", "Press the Android Back button.") {
            val ok = bridge.performKey(4)
            McpCallResult(content = listOf(McpContent(text = if (ok) "Back pressed" else "Failed")))
        }
        register("android_press_home", "Press the Android Home button.") {
            val ok = bridge.performKey(3)
            McpCallResult(content = listOf(McpContent(text = if (ok) "Home pressed" else "Failed")))
        }
        register("android_press_recents", "Press the Android Overview/Recents button.") {
            val ok = bridge.performKey(187)
            McpCallResult(content = listOf(McpContent(text = if (ok) "Recents opened" else "Failed")))
        }
    }

    private fun registerKeyboardTools() {
        register("android_type_append_text", "Type text into currently focused input field.") { args ->
            val text = args.optString("text", "")
            val ok = bridge.typeText(text)
            McpCallResult(content = listOf(McpContent(text = if (ok) "Typed '$text'" else "Type failed")))
        }
        register("android_dismiss_keyboard", "Dismiss the virtual on-screen soft keyboard.") {
            val ok = bridge.performKey(4)
            McpCallResult(content = listOf(McpContent(text = if (ok) "Keyboard dismissed" else "Dismiss failed")))
        }
    }

    private fun registerSystemTools() {
        register("android_get_clipboard", "Read current clipboard text content.") {
            val clip = bridge.getClipboard() ?: ""
            McpCallResult(content = listOf(McpContent(text = clip)))
        }
        register("android_set_clipboard", "Write text to device clipboard.") { args ->
            val text = args.optString("text", "")
            val ok = bridge.setClipboard(text)
            McpCallResult(content = listOf(McpContent(text = if (ok) "Clipboard updated" else "Clipboard set failed")))
        }
        register("android_open_app", "Launch application by package name.") { args ->
            val pkg = args.optString("package_name", "")
            val ok = bridge.openApp(pkg)
            McpCallResult(content = listOf(McpContent(text = if (ok) "Launched package '$pkg'" else "Failed to launch package '$pkg'")))
        }
        register("android_get_device_logs", "Retrieve latest logcat lines.") {
            val logs = bridge.getDeviceLogs()
            McpCallResult(content = listOf(McpContent(text = logs)))
        }
        register("android_notification_list", "List active status bar notifications.") {
            val notifications = bridge.getNotifications()
            McpCallResult(content = listOf(McpContent(text = notifications.toString(2))))
        }
    }
}
