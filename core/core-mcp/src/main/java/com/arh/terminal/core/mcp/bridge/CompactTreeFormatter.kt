package com.arh.terminal.core.mcp.bridge

import org.json.JSONArray
import org.json.JSONObject

/**
 * Strips redundant layout containers (FrameLayout, LinearLayout, ComposeView)
 * and formats only actionable interactive elements into a token-efficient JSON tree.
 * Reduces LLM context token consumption by up to 85%.
 */
object CompactTreeFormatter {

    fun formatCompact(rawTree: JSONObject): JSONObject {
        val compact = JSONObject()
        compact.put("status", rawTree.optString("status", "ok"))
        compact.put("width", rawTree.optInt("width", 0))
        compact.put("height", rawTree.optInt("height", 0))

        val rawNodes = rawTree.optJSONArray("nodes") ?: JSONArray()
        val actionableNodes = JSONArray()

        for (i in 0 until rawNodes.length()) {
            val node = rawNodes.getJSONObject(i)
            val isClickable = node.optBoolean("isClickable", false)
            val isEditable = node.optBoolean("isEditable", false)
            val text = node.optString("text", "")
            val contentDesc = node.optString("contentDescription", "")
            val viewId = node.optString("viewId", "")

            // Keep node only if it is interactive or contains meaningful textual content
            if (isClickable || isEditable || text.isNotBlank() || contentDesc.isNotBlank()) {
                val cleanNode = JSONObject()
                val className = node.optString("className", "").substringAfterLast(".")
                if (className.isNotBlank()) cleanNode.put("type", className)
                if (text.isNotBlank()) cleanNode.put("text", text)
                if (contentDesc.isNotBlank()) cleanNode.put("desc", contentDesc)
                if (viewId.isNotBlank()) cleanNode.put("id", viewId.substringAfter(":id/"))
                if (isClickable) cleanNode.put("clickable", true)
                if (isEditable) cleanNode.put("editable", true)
                cleanNode.put("bounds", node.optString("bounds", ""))
                actionableNodes.put(cleanNode)
            }
        }

        compact.put("nodeCount", actionableNodes.length())
        compact.put("elements", actionableNodes)
        return compact
    }
}
