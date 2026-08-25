package com.arh.terminal.data.github

import android.content.Context
import android.content.SharedPreferences
import com.arh.terminal.data.security.CredentialCrypto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // GitHub CLI / Public App Client ID for RFC 8628 Device Flow
        private const val GITHUB_CLIENT_ID = "Iv1.b507a08c87ecfe98"
        private const val PREFS_NAME = "arh_terminal_github_auth"
        private const val KEY_ENCRYPTED_TOKEN = "github_oauth_token_encrypted"
        private const val MIN_WAIT_SECONDS = 5
        private const val MAX_POLL_ATTEMPTS = 60
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun requestDeviceCode(): Result<GitHubDeviceAuth> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://github.com/login/device/code")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("client_id", GITHUB_CLIENT_ID)
                put("scope", "repo,read:org,read:user")
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(responseText)
                Result.success(
                    GitHubDeviceAuth(
                        deviceCode = json.getString("device_code"),
                        userCode = json.getString("user_code"),
                        verificationUri = json.getString("verification_uri"),
                        expiresIn = json.getInt("expires_in"),
                        interval = json.optInt("interval", MIN_WAIT_SECONDS)
                    )
                )
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP ${conn.responseCode}"
                Result.failure(Exception("Failed to request device code: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pollForAccessToken(deviceCode: String, intervalSeconds: Int = MIN_WAIT_SECONDS): Result<String> = withContext(Dispatchers.IO) {
        var attempts = 0
        var waitSec = intervalSeconds.coerceAtLeast(MIN_WAIT_SECONDS)

        while (attempts < MAX_POLL_ATTEMPTS) {
            delay(waitSec * 1000L)
            attempts++

            try {
                val url = URL("https://github.com/login/oauth/access_token")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }

                val payload = JSONObject().apply {
                    put("client_id", GITHUB_CLIENT_ID)
                    put("device_code", deviceCode)
                    put("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                }

                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

                if (conn.responseCode in 200..299) {
                    val responseText = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(responseText)
                    if (json.has("access_token")) {
                        val token = json.getString("access_token")
                        saveToken(token)
                        return@withContext Result.success(token)
                    }
                    val error = json.optString("error", "")
                    if (error == "slow_down") {
                        waitSec += MIN_WAIT_SECONDS
                    } else if (error != "authorization_pending") {
                        return@withContext Result.failure(Exception("Authorization error: $error"))
                    }
                }
            } catch (_: Exception) {
                // Continue polling until timeout
            }
        }
        Result.failure(Exception("Device authentication timed out."))
    }

    fun saveToken(token: String) {
        val encrypted = CredentialCrypto.encrypt(token)
        prefs.edit().putString(KEY_ENCRYPTED_TOKEN, encrypted).apply()
    }

    fun getStoredToken(): String? {
        val encrypted = prefs.getString(KEY_ENCRYPTED_TOKEN, null) ?: return null
        val decrypted = CredentialCrypto.decrypt(encrypted)
        return decrypted.ifBlank { null }
    }

    fun isAuthorized(): Boolean {
        return !getStoredToken().isNullOrBlank()
    }

    fun logout() {
        prefs.edit().remove(KEY_ENCRYPTED_TOKEN).apply()
    }
}
