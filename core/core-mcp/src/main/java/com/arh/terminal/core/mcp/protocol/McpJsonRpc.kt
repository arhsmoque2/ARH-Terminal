package com.arh.terminal.core.mcp.protocol

import org.json.JSONObject

data class McpToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: JSONObject = JSONObject()
)

data class McpCallResult(
    val isError: Boolean = false,
    val content: List<McpContent> = emptyList()
)

data class McpContent(
    val type: String = "text",
    val text: String? = null,
    val data: String? = null,
    val mimeType: String? = null
)
