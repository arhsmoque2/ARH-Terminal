package com.arh.terminal.data.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AgentAuditJournalTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun auditJournalPersistsActionsAcrossRestarts() {
        val file = tempFolder.newFile("audit_journal.tsv")

        val journal1 = AgentAuditJournal()
        journal1.configureStorage(file)
        journal1.recordAction(
            agentName = "Claude Code",
            actionType = "git_commit",
            details = "git commit -m 'feat: update parser'",
            tier = ConsentTier.MUTATIVE,
            approved = true
        )

        // Session 2: Cold restart
        val journal2 = AgentAuditJournal()
        journal2.configureStorage(file)
        val loaded = journal2.records.value

        assertEquals(1, loaded.size)
        assertEquals("Claude Code", loaded[0].agentName)
        assertEquals(ConsentTier.MUTATIVE, loaded[0].tier)
        assertTrue(loaded[0].approvedByUser)
    }
}
