package com.arh.terminal.core.mcp.bridge

import org.json.JSONArray
import org.json.JSONObject

interface AccessibilityBridge {
    fun getScreenState(): JSONObject
    fun findNodes(query: String): JSONArray
    fun performTap(x: Int, y: Int): Boolean
    fun performSwipe(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Long): Boolean
    fun performKey(keyCode: Int): Boolean
    fun typeText(text: String): Boolean
    fun getClipboard(): String?
    fun setClipboard(text: String): Boolean
}

class DefaultAccessibilityBridge : AccessibilityBridge {
    private var clipboardContent: String = ""

    override fun getScreenState(): JSONObject {
        return JSONObject().apply {
            put("status", "service_not_connected")
            put("nodesCount", 0)
            put("nodes", JSONArray())
        }
    }

    override fun findNodes(query: String): JSONArray = JSONArray()
    override fun performTap(x: Int, y: Int): Boolean = true
    override fun performSwipe(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Long): Boolean = true
    override fun performKey(keyCode: Int): Boolean = true
    override fun typeText(text: String): Boolean = true
    override fun getClipboard(): String = clipboardContent
    override fun setClipboard(text: String): Boolean {
        clipboardContent = text
        return true
    }
}
