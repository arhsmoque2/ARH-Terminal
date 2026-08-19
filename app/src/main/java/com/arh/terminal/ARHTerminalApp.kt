package com.arh.terminal

import android.app.Application
import com.arh.terminal.data.audit.AgentAuditJournal
import com.arh.terminal.data.profiles.ProfileRepository
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class ARHTerminalApp : Application() {

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var auditJournal: AgentAuditJournal

    @Inject
    lateinit var knownHostsStore: com.arh.terminal.data.security.KnownHostsStore

    override fun onCreate() {
        super.onCreate()
        val profilesFile = File(filesDir, "arh_profiles.tsv")
        profileRepository.configureStorage(profilesFile)

        val auditFile = File(filesDir, "arh_audit_journal.tsv")
        auditJournal.configureStorage(auditFile)

        knownHostsStore.initialize(this)
    }
}
