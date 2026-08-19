package com.arh.terminal.core.mcp.bridge

import org.json.JSONObject
import java.security.MessageDigest

/**
 * Generates deterministic SHA-256 fingerprint hashes of visible UI element hierarchies
 * to enable zero-latency UI settle detection for android_wait_for_idle.
 */
object TreeFingerprint {

    fun computeFingerprint(screenState: JSONObject): String {
        val elements = screenState.optJSONArray("elements") 
            ?: screenState.optJSONArray("nodes") 
            ?: return "empty"

        val sb = StringBuilder()
        for (i in 0 until elements.length()) {
            val el = elements.optJSONObject(i) ?: continue
            sb.append(el.optString("type"))
            sb.append(el.optString("text"))
            sb.append(el.optString("bounds"))
            sb.append(el.optString("id"))
            sb.append("|")
        }

        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(sb.toString().toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
