package com.arh.terminal.core.relay.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class RelayCipher(passphrase: String) {
    private val keySpec: SecretKeySpec
    private val secureRandom = SecureRandom()

    init {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val keyBytes = sha256.digest(passphrase.toByteArray(Charsets.UTF_8))
        keySpec = SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plaintext: String): EncryptedPayload {
        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(
            ciphertext = Base64.getEncoder().encodeToString(ciphertext),
            iv = Base64.getEncoder().encodeToString(iv)
        )
    }

    fun decrypt(payload: EncryptedPayload): String {
        val iv = Base64.getDecoder().decode(payload.iv)
        val ciphertext = Base64.getDecoder().decode(payload.ciphertext)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)

        val decrypted = cipher.doFinal(ciphertext)
        return String(decrypted, Charsets.UTF_8)
    }
}

data class EncryptedPayload(
    val ciphertext: String,
    val iv: String
)
