package com.arh.terminal.core.relay.client

import com.arh.terminal.core.relay.crypto.EncryptedPayload
import com.arh.terminal.core.relay.crypto.RelayCipher
import com.arh.terminal.core.relay.protocol.RelayEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

sealed interface RelayStatus {
    data object Disconnected : RelayStatus
    data class Connecting(val url: String) : RelayStatus
    data class Connected(val url: String) : RelayStatus
    data class Error(val error: String) : RelayStatus
}

class RelayWebSocketClient(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val okHttpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var activeWebSocket: WebSocket? = null
    private var cipher: RelayCipher? = null
    private var activeSessionId: String = ""

    private val _status = MutableStateFlow<RelayStatus>(RelayStatus.Disconnected)
    val status: StateFlow<RelayStatus> = _status.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 128)
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    fun connect(relayUrl: String, secretKey: String, sessionId: String = "arh-relay-session") {
        cipher = RelayCipher(secretKey)
        activeSessionId = sessionId
        _status.update { RelayStatus.Connecting(relayUrl) }

        val request = Request.Builder()
            .url(relayUrl)
            .build()

        activeWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _status.update { RelayStatus.Connected(relayUrl) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val envelope = RelayEnvelope.fromJson(text) ?: return
                val currentCipher = cipher ?: return
                try {
                    val decrypted = currentCipher.decrypt(EncryptedPayload(envelope.ciphertext, envelope.iv))
                    scope.launch {
                        _incomingMessages.emit(decrypted)
                    }
                } catch (_: Exception) { }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _status.update { RelayStatus.Error(t.message ?: "WebSocket connection failed") }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _status.update { RelayStatus.Disconnected }
            }
        })
    }

    fun send(message: String, type: String = "data"): Boolean {
        val ws = activeWebSocket ?: return false
        val currentCipher = cipher ?: return false
        val encrypted = currentCipher.encrypt(message)
        val envelope = RelayEnvelope.wrap(activeSessionId, type, encrypted)
        return ws.send(envelope.toJson())
    }

    fun disconnect() {
        activeWebSocket?.close(1000, "Client disconnect")
        activeWebSocket = null
        cipher = null
        _status.update { RelayStatus.Disconnected }
    }
}
