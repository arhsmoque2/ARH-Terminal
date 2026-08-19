package com.arh.terminal.data.audit

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class ConsentTier {
    READ_ONLY,
    MUTATIVE,
    CRITICAL
}

@Immutable
data class AuditRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    val agentName: String,
    val actionType: String,
    val details: String,
    val tier: ConsentTier,
    val approvedByUser: Boolean = true
) {
    fun toSerialized(): String {
        return listOf(id, timestamp, agentName, actionType, details.replace("	", " "), tier.name, approvedByUser.toString())
            .joinToString("	")
    }

    companion object {
        fun fromSerialized(line: String): AuditRecord? {
            val parts = line.split("	")
            if (parts.size < 7) return null
            return AuditRecord(
                id = parts[0],
                timestamp = parts[1],
                agentName = parts[2],
                actionType = parts[3],
                details = parts[4],
                tier = try { ConsentTier.valueOf(parts[5]) } catch (_: Exception) { ConsentTier.MUTATIVE },
                approvedByUser = parts[6].toBoolean()
            )
        }
    }
}

@Singleton
class AgentAuditJournal @Inject constructor() {
    private var storageFile: File? = null
    private val _records = MutableStateFlow<List<AuditRecord>>(emptyList())
    val records: StateFlow<List<AuditRecord>> = _records.asStateFlow()

    fun configureStorage(file: File) {
        storageFile = file
        loadFromDisk()
    }

    fun recordAction(agentName: String, actionType: String, details: String, tier: ConsentTier, approved: Boolean = true) {
        val record = AuditRecord(
            agentName = agentName,
            actionType = actionType,
            details = details,
            tier = tier,
            approvedByUser = approved
        )
        _records.update { list -> (listOf(record) + list).take(200) }
        saveToDisk()
    }

    fun clearJournal() {
        _records.value = emptyList()
        storageFile?.writeText("", Charsets.UTF_8)
    }

    private fun loadFromDisk() {
        val file = storageFile ?: return
        if (file.exists() && file.length() > 0) {
            try {
                val lines = file.readLines(Charsets.UTF_8)
                val loaded = lines.mapNotNull { AuditRecord.fromSerialized(it) }
                _records.value = loaded
            } catch (_: Exception) { }
        }
    }

    private fun saveToDisk() {
        val file = storageFile ?: return
        try {
            val content = _records.value.joinToString("\n") { it.toSerialized() }
            file.writeText(content, Charsets.UTF_8)
        } catch (_: Exception) { }
    }
}
