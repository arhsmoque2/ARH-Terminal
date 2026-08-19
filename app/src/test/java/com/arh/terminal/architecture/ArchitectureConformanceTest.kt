package com.arh.terminal.architecture

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

/**
 * Architectural invariant tests ensuring security policies and secret hygiene.
 */
class ArchitectureConformanceTest {

    @Test
    fun noHardcodedMcpBearerTokensInProductionCode() {
        val root = File("src/main/java")
        if (!root.exists()) return

        val sourceFiles = root.walkTopDown().filter { it.extension == "kt" }.toList()
        val forbiddenTokens = listOf(
            "ca48ffe8-cb63-45be-bfd5-1911e367fbcd",
            "default-insecure-secret"
        )

        for (file in sourceFiles) {
            val content = file.readText(Charsets.UTF_8)
            for (forbidden in forbiddenTokens) {
                assertFalse(
                    "Forbidden hardcoded token '$forbidden' found in ${file.name}",
                    content.contains(forbidden)
                )
            }
        }
    }
}
