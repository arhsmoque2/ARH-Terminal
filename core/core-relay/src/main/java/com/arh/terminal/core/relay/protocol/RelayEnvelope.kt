package com.arh.terminal.core.relay.protocol

import com.arh.terminal.core.relay.crypto.EncryptedPayload
import org.json.JSONObject

data class RelayEnvelope(
    val sessionId: String,
    val type: String,
    val ciphertext: String,
    val iv: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("sessionId", sessionId)
            put("type", type)
            put("ciphertext", ciphertext)
            put("iv", iv)
            put("timestamp", timestamp)
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): RelayEnvelope? {
            return try {
                val json = JSONObject(jsonStr)
                RelayEnvelope(
                    sessionId = json.getString("sessionId"),
                    type = json.getString("type"),
                    ciphertext = json.getString("ciphertext"),
                    iv = json.getString("iv"),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis())
                )
            } catch (_: Exception) {
                null
            }
        }

        fun wrap(sessionId: String, type: String, payload: EncryptedPayload): RelayEnvelope {
            return RelayEnvelope(
                sessionId = sessionId,
                type = type,
                ciphertext = payload.ciphertext,
                iv = payload.iv
            )
        }
    }
}
