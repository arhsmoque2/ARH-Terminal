package com.arh.terminal.data.github

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GitHubAuthManagerTest {

    private val storage = mutableMapOf<String, String>()
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val prefs = mockk<SharedPreferences>(relaxed = true)
    private val context = mockk<Context>()
    private lateinit var authManager: GitHubAuthManager

    @Before
    fun setUp() {
        storage.clear()
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { prefs.getString(any(), any()) } answers {
            val key = firstArg<String>()
            val def = secondArg<String?>()
            storage[key] ?: def
        }
        every { editor.putString(any(), any()) } answers {
            val key = firstArg<String>()
            val value = secondArg<String>()
            storage[key] = value
            editor
        }
        every { editor.remove(any()) } answers {
            val key = firstArg<String>()
            storage.remove(key)
            editor
        }
        every { editor.apply() } returns Unit

        authManager = GitHubAuthManager(context)
    }

    @Test
    fun saveToken_and_getStoredToken_roundtrip() {
        assertFalse(authManager.isAuthorized())
        assertNull(authManager.getStoredToken())

        val token = "gho_test_secret_token_12345"
        authManager.saveToken(token)

        assertTrue(authManager.isAuthorized())
        assertEquals(token, authManager.getStoredToken())

        authManager.logout()
        assertFalse(authManager.isAuthorized())
        assertNull(authManager.getStoredToken())
    }
}
