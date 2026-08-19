package com.arh.terminal.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialCryptoTest {

    @Test
    fun encryptAndDecryptRoundTrip() {
        val testPem = "-----BEGIN OPENSSH PRIVATE KEY-----\ntest-key-material\n-----END OPENSSH PRIVATE KEY-----"
        val encrypted = CredentialCrypto.encrypt(testPem)

        assertTrue(encrypted.isNotBlank())
        assertTrue(encrypted.startsWith("ks:") || encrypted.startsWith("enc:"))

        val decrypted = CredentialCrypto.decrypt(encrypted)
        assertEquals(testPem, decrypted)
    }

    @Test
    fun decryptsLegacyBase64Format() {
        val plainKey = "legacy-private-key"
        val legacyEnc = "enc:" + java.util.Base64.getEncoder().encodeToString(plainKey.toByteArray(Charsets.UTF_8))

        val decrypted = CredentialCrypto.decrypt(legacyEnc)
        assertEquals(plainKey, decrypted)
    }

    @Test
    fun handlesEmptyOrPlaintextGracefully() {
        assertEquals("", CredentialCrypto.encrypt(""))
        assertEquals("", CredentialCrypto.decrypt(""))
        assertEquals("raw-plaintext", CredentialCrypto.decrypt("raw-plaintext"))
    }
}
