package com.arh.terminal.data.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.PublicKey

class TofuHostKeyVerifierTest {

    private val store: KnownHostsStore = mockk(relaxed = true)

    @Test
    fun verifyFirstSeenStoresFingerprintAndReturnsTrue() {
        val mockKey: PublicKey = mockk()
        every { store.fingerprint(mockKey) } returns "SHA256:abcd1234efgh"
        every { store.getStored("192.168.1.50", 22) } returns null

        val messages = mutableListOf<String>()
        val verifier = TofuHostKeyVerifier(store) { messages.add(it) }

        val allowed = verifier.verify("192.168.1.50", 22, mockKey)

        assertTrue(allowed)
        verify { store.store("192.168.1.50", 22, "SHA256:abcd1234efgh") }
        assertTrue(messages.any { it.contains("Trusted new host key") })
    }

    @Test
    fun verifyMatchingKeyReturnsTrueWithoutReStoring() {
        val mockKey: PublicKey = mockk()
        every { store.fingerprint(mockKey) } returns "SHA256:abcd1234efgh"
        every { store.getStored("192.168.1.50", 22) } returns "SHA256:abcd1234efgh"

        val verifier = TofuHostKeyVerifier(store)
        val allowed = verifier.verify("192.168.1.50", 22, mockKey)

        assertTrue(allowed)
    }

    @Test
    fun verifyMismatchedKeyRejectsAndAlerts() {
        val mockKey: PublicKey = mockk()
        every { store.fingerprint(mockKey) } returns "SHA256:evil_fingerprint"
        every { store.getStored("192.168.1.50", 22) } returns "SHA256:legit_fingerprint"

        val messages = mutableListOf<String>()
        val verifier = TofuHostKeyVerifier(store) { messages.add(it) }

        val allowed = verifier.verify("192.168.1.50", 22, mockKey)

        assertFalse(allowed)
        assertTrue(messages.any { it.contains("SECURITY ALERT") && it.contains("CHANGED") })
    }
}
