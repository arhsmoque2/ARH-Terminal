package com.arh.terminal.data.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProfileRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun profilesPersistAcrossColdRestart() {
        val storageFile = tempFolder.newFile("profiles.json")

        // 1. Session 1: Add new profile
        val repo1 = ProfileRepository()
        repo1.configureStorage(storageFile)
        val newDevBox = ConnectionProfile(
            name = "Tailscale Host PC",
            host = "100.85.219.219",
            port = 22,
            username = "agent-dev"
        )
        repo1.addProfile(newDevBox)

        // 2. Session 2: Cold restart simulation (new repository instance)
        val repo2 = ProfileRepository()
        repo2.configureStorage(storageFile)
        val restored = repo2.profiles.value

        assertTrue("Restored profiles must contain newly added Tailscale devbox", restored.any { it.name == "Tailscale Host PC" })
        assertEquals("100.85.219.219", restored.first { it.name == "Tailscale Host PC" }.host)
    }
}
