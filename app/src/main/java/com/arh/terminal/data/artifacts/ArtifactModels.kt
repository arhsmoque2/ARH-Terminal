package com.arh.terminal.data.artifacts

import androidx.compose.runtime.Immutable

@Immutable
data class AgentArtifact(
    val id: String,
    val fileName: String,
    val fileType: String,
    val previewContent: String,
    val sourceContent: String,
    val sizeFormatted: String,
    val timestamp: Long = System.currentTimeMillis()
)
