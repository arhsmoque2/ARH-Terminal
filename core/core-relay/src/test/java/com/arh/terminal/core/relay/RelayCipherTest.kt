package com.arh.terminal.core.relay

import com.arh.terminal.core.relay.crypto.RelayCipher
import com.arh.terminal.core.relay.protocol.RelayEnvelope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RelayCipherTest {

    private val cipher = RelayCipher("my-super-secret-key-12345")

    @Test
    fun encryptAndDecryptRoundTrip() {
        val original = "send-keys -t %0 \"cargo build --release\" Enter"
        val encrypted = cipher.encrypt(original)

        assertNotNull(encrypted.ciphertext)
        assertNotNull(encrypted.iv)
        assertNotEquals(original, encrypted.ciphertext)

        val decrypted = cipher.decrypt(encrypted)
        assertEquals(original, decrypted)
    }

    @Test
    fun relayEnvelopeSerializationRoundTrip() {
        val payload = cipher.encrypt("hello-e2ee-relay")
        val envelope = RelayEnvelope.wrap("session-abc", "tmux-feed", payload)

        val jsonStr = envelope.toJson()
        val parsed = RelayEnvelope.fromJson(jsonStr)

        assertNotNull(parsed)
        assertEquals("session-abc", parsed?.sessionId)
        assertEquals("tmux-feed", parsed?.type)
        assertEquals(payload.ciphertext, parsed?.ciphertext)

        val decrypted = cipher.decrypt(payload)
        assertEquals("hello-e2ee-relay", decrypted)
    }
}
