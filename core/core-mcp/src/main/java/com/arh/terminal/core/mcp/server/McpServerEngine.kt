package com.arh.terminal.core.mcp.server

import com.arh.terminal.core.mcp.tools.AndroidToolRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

data class McpServerStats(
    val isRunning: Boolean = false,
    val port: Int = 8070,
    val bearerToken: String = UUID.randomUUID().toString(),
    val activeSessions: Int = 0,
    val totalToolCalls: Int = 0,
    val lastToolCall: String? = null
)

class McpServerEngine(
    val toolRegistry: AndroidToolRegistry,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val activeSessions = mutableSetOf<String>()

    private val _stats = MutableStateFlow(McpServerStats())
    val stats: StateFlow<McpServerStats> = _stats.asStateFlow()

    fun start(port: Int = 8070, token: String = UUID.randomUUID().toString()) {
        if (_stats.value.isRunning) return
        _stats.update { it.copy(port = port, bearerToken = token) }

        serverJob = scope.launch {
            try {
                val ss = ServerSocket(port)
                serverSocket = ss
                _stats.update { it.copy(isRunning = true) }

                while (true) {
                    val client = ss.accept()
                    launch { handleClient(client) }
                }
            } catch (_: Exception) {
                stop()
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) { }
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
        _stats.update { it.copy(isRunning = false, activeSessions = 0) }
    }

    private suspend fun handleClient(socket: Socket) {
        socket.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
            val out = s.getOutputStream()

            val line = reader.readLine() ?: return
            val parts = line.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val path = parts[1]

            var authHeader: String? = null
            var sessionIdHeader: String? = null
            var contentLength = 0

            var headerLine = reader.readLine()
            while (!headerLine.isNullOrBlank()) {
                val lower = headerLine.lowercase()
                if (lower.startsWith("authorization:")) {
                    authHeader = headerLine.substringAfter(":").trim()
                } else if (lower.startsWith("mcp-session-id:")) {
                    sessionIdHeader = headerLine.substringAfter(":").trim()
                } else if (lower.startsWith("content-length:")) {
                    contentLength = headerLine.substringAfter(":").trim().toIntOrNull() ?: 0
                }
                headerLine = reader.readLine()
            }

            // Verify Bearer Token
            val expectedAuth = "Bearer ${_stats.value.bearerToken}"
            if (authHeader != expectedAuth) {
                sendResponse(out, 401, "{\"error\": \"Unauthorized: Invalid Bearer Token\"}")
                return
            }

            if (method == "POST" && path.startsWith("/mcp")) {
                val bodyChars = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val r = reader.read(bodyChars, read, contentLength - read)
                    if (r == -1) break
                    read += r
                }
                val body = String(bodyChars, 0, read)
                val json = try { JSONObject(body) } catch (_: Exception) { null }
                if (json == null) {
                    sendResponse(out, 400, "{\"error\": \"Invalid JSON\"}")
                    return
                }

                val responseJson = processJsonRpc(json)
                val sessionId = sessionIdHeader ?: UUID.randomUUID().toString()
                activeSessions.add(sessionId)
                _stats.update { it.copy(activeSessions = activeSessions.size) }

                sendResponse(out, 200, responseJson.toString(), sessionId)
            } else {
                sendResponse(out, 404, "{\"error\": \"Not Found\"}")
            }
        }
    }

    private suspend fun processJsonRpc(req: JSONObject): JSONObject {
        val id = req.opt("id")
        val method = req.optString("method")
        val params = req.optJSONObject("params") ?: JSONObject()

        val resp = JSONObject()
        resp.put("jsonrpc", "2.0")
        if (id != null) resp.put("id", id)

        when (method) {
            "initialize" -> {
                val result = JSONObject().apply {
                    put("protocolVersion", "2024-11-05")
                    put("serverInfo", JSONObject().apply {
                        put("name", "arh-terminal-mcp")
                        put("version", "1.0.0")
                    })
                    put("capabilities", JSONObject().apply {
                        put("tools", JSONObject().apply { put("listChanged", false) })
                    })
                }
                resp.put("result", result)
            }
            "notifications/initialized" -> {
                resp.put("result", JSONObject().apply { put("status", "ok") })
            }
            "tools/list" -> {
                val toolList = toolRegistry.listTools()
                val array = JSONArray()
                for (t in toolList) {
                    array.put(JSONObject().apply {
                        put("name", t.name)
                        put("description", t.description)
                        put("inputSchema", t.inputSchema)
                    })
                }
                resp.put("result", JSONObject().apply { put("tools", array) })
            }
            "tools/call" -> {
                val name = params.optString("name")
                val args = params.optJSONObject("arguments") ?: JSONObject()

                _stats.update {
                    it.copy(totalToolCalls = it.totalToolCalls + 1, lastToolCall = name)
                }

                val callRes = toolRegistry.executeTool(name, args)
                val contentArray = JSONArray()
                for (c in callRes.content) {
                    contentArray.put(JSONObject().apply {
                        put("type", c.type)
                        c.text?.let { put("text", it) }
                        c.data?.let { put("data", it) }
                        c.mimeType?.let { put("mimeType", it) }
                    })
                }
                val resultObj = JSONObject().apply {
                    put("content", contentArray)
                    put("isError", callRes.isError)
                }
                resp.put("result", resultObj)
            }
            else -> {
                resp.put("error", JSONObject().apply {
                    put("code", -32601)
                    put("message", "Method '$method' not found")
                })
            }
        }
        return resp
    }

    private fun sendResponse(out: OutputStream, status: Int, body: String, sessionId: String? = null) {
        val statusText = if (status == 200) "OK" else if (status == 401) "Unauthorized" else "Error"
        val bytes = body.toByteArray(Charsets.UTF_8)
        val sb = StringBuilder()
        sb.append("HTTP/1.1 $status $statusText\r\n")
        sb.append("Content-Type: application/json\r\n")
        sb.append("Content-Length: ${bytes.size}\r\n")
        sb.append("Connection: close\r\n")
        if (sessionId != null) {
            sb.append("mcp-session-id: $sessionId\r\n")
        }
        sb.append("\r\n")
        out.write(sb.toString().toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }
}
