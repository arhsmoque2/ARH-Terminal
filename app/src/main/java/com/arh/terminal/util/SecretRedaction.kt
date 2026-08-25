package com.arh.terminal.util

/**
 * Scrubs credentials embedded in URLs (`scheme://user:token@host/...` or
 * `scheme://token@host/...`) before text reaches a persistent or shareable sink — the audit
 * journal, a Markdown session export, etc.
 *
 * This exists because ARH-Terminal sends commands as literal keystrokes to a real remote
 * shell (so the shell's own echo, not this app, decides what appears in the live terminal
 * feed — that's unavoidable for an actual terminal). What *is* avoidable is this app's own
 * copies of that text: the audit journal and session exports are ARH-Terminal's own
 * persistent/shareable artifacts, and a credential like the GitHub device-flow token embedded
 * in a private-repo `git clone https://x-access-token:<token>@github.com/...` should not sit
 * in either of them in plaintext.
 */
object SecretRedaction {
    private val CREDENTIAL_IN_URL = Regex("(?<=://)[^/\\s@]+(?=@)")

    fun redact(text: String): String = CREDENTIAL_IN_URL.replace(text, "***REDACTED***")
}
