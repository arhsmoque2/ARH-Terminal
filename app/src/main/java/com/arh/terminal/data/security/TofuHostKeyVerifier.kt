package com.arh.terminal.data.security

import android.content.Context
import android.util.Base64
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import java.security.MessageDigest
import java.security.PublicKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnownHostsStore @Inject constructor() {
    private var context: Context? = null

    fun initialize(ctx: Context) {
        context = ctx.applicationContext
    }

    private val prefs by lazy {
        val ctx = checkNotNull(context) { "KnownHostsStore must be initialized with Context" }
        ctx.getSharedPreferences("arh_known_hosts", Context.MODE_PRIVATE)
    }

    fun fingerprint(key: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.encoded)
        return "SHA256:" + Base64.encodeToString(digest, Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun getStored(host: String, port: Int): String? {
        return if (context != null) {
            prefs.getString("$host:$port", null)
        } else {
            null
        }
    }

    fun store(host: String, port: Int, fingerprint: String) {
        if (context != null) {
            prefs.edit().putString("$host:$port", fingerprint).apply()
        }
    }

    fun forget(host: String, port: Int) {
        if (context != null) {
            prefs.edit().remove("$host:$port").apply()
        }
    }
}

/**
 * Trust-On-First-Use (TOFU) Host Key Verifier.
 * Pins host key fingerprint on initial connect and strictly rejects mismatches to protect against MITM.
 */
class TofuHostKeyVerifier(
    private val store: KnownHostsStore,
    private val onMessage: (String) -> Unit = {}
) : HostKeyVerifier {

    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
        val id = "$hostname:$port"
        val fp = store.fingerprint(key)
        val saved = store.getStored(hostname, port)

        return when {
            saved == null -> {
                store.store(hostname, port, fp)
                onMessage("Trusted new host key for $id ($fp)")
                true
            }
            saved == fp -> true
            else -> {
                onMessage("SECURITY ALERT: Host key for $id has CHANGED! Expected $saved, received $fp. Rejecting connection.")
                false
            }
        }
    }

    override fun findExistingAlgorithms(hostname: String, port: Int): List<String> = emptyList()
}
