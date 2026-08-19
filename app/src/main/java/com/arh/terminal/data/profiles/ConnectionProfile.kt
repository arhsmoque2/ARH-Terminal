package com.arh.terminal.data.profiles

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
data class ConnectionProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val defaultSession: String = "arh-agent",
    val privateKeyPem: String = ""
) {
    fun toSerialized(): String {
        val safeKey = if (privateKeyPem.isNotBlank()) {
            val encoded = java.util.Base64.getEncoder().encodeToString(privateKeyPem.toByteArray(Charsets.UTF_8))
            "enc:$encoded"
        } else {
            ""
        }
        return listOf(id, name, host, port.toString(), username, defaultSession, safeKey)
            .joinToString("\t")
    }

    companion object {
        fun fromSerialized(line: String): ConnectionProfile? {
            val parts = line.split("\t")
            if (parts.size < 5) return null
            val rawKey = if (parts.size > 6) parts[6] else ""
            val resolvedKey = if (rawKey.startsWith("enc:")) {
                try {
                    String(java.util.Base64.getDecoder().decode(rawKey.removePrefix("enc:")), Charsets.UTF_8)
                } catch (_: Exception) {
                    rawKey
                }
            } else {
                rawKey
            }
            return ConnectionProfile(
                id = parts[0],
                name = parts[1],
                host = parts[2],
                port = parts[3].toIntOrNull() ?: 22,
                username = parts[4],
                defaultSession = if (parts.size > 5) parts[5] else "arh-agent",
                privateKeyPem = resolvedKey
            )
        }
    }
}

@Singleton
class ProfileRepository @Inject constructor() {
    private var storageFile: File? = null
    private val _profiles = MutableStateFlow<List<ConnectionProfile>>(emptyList())
    val profiles: StateFlow<List<ConnectionProfile>> = _profiles.asStateFlow()

    init {
        loadDefaultOrPersisted()
    }

    fun configureStorage(file: File) {
        storageFile = file
        loadDefaultOrPersisted()
    }

    fun addProfile(profile: ConnectionProfile) {
        _profiles.update { list -> (list.filterNot { it.id == profile.id } + profile) }
        saveToDisk()
    }

    fun removeProfile(id: String) {
        _profiles.update { list -> list.filterNot { it.id == id } }
        saveToDisk()
    }

    private fun loadDefaultOrPersisted() {
        val file = storageFile
        if (file != null && file.exists() && file.length() > 0) {
            try {
                val lines = file.readLines(Charsets.UTF_8)
                val loaded = lines.mapNotNull { ConnectionProfile.fromSerialized(it) }
                if (loaded.isNotEmpty()) {
                    _profiles.value = loaded
                    return
                }
            } catch (_: Exception) { }
        }

        // Fallback default devboxes
        _profiles.value = listOf(
            ConnectionProfile(
                id = "default-win-box",
                name = "Windows DevBox (psmux)",
                host = "127.0.0.1",
                port = 22,
                username = "Administrator",
                defaultSession = "arh-agent"
            ),
            ConnectionProfile(
                id = "default-runpod",
                name = "RunPod GPU Remote",
                host = "ssh.runpod.io",
                port = 22,
                username = "root",
                defaultSession = "agent-build"
            )
        )
    }

    private fun saveToDisk() {
        val file = storageFile ?: return
        try {
            val content = _profiles.value.joinToString("\n") { it.toSerialized() }
            file.writeText(content, Charsets.UTF_8)
        } catch (_: Exception) { }
    }
}
