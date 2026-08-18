package com.arh.terminal.data.profiles

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
)

@Singleton
class ProfileRepository @Inject constructor() {
    private val _profiles = MutableStateFlow(
        listOf(
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
    )
    val profiles: StateFlow<List<ConnectionProfile>> = _profiles.asStateFlow()

    fun addProfile(profile: ConnectionProfile) {
        _profiles.update { it + profile }
    }

    fun removeProfile(id: String) {
        _profiles.update { list -> list.filterNot { it.id == id } }
    }
}
