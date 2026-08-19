package com.arh.terminal.ui.session

import com.arh.terminal.core.mcp.server.McpServerEngine
import com.arh.terminal.core.relay.client.RelayWebSocketClient
import com.arh.terminal.data.profiles.ProfileRepository
import com.arh.terminal.util.NetworkMonitor
import com.pocketshell.core.tmux.TmuxClientFactory
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val factory: TmuxClientFactory = mockk(relaxed = true)
    private val profileRepository: ProfileRepository = ProfileRepository()
    private val networkMonitor: NetworkMonitor = mockk(relaxed = true)
    private val mcpServerEngine: McpServerEngine = mockk(relaxed = true)
    private val relayClient: RelayWebSocketClient = mockk(relaxed = true)
    private val auditJournal: com.arh.terminal.data.audit.AgentAuditJournal = com.arh.terminal.data.audit.AgentAuditJournal()
    private val knownHostsStore: com.arh.terminal.data.security.KnownHostsStore = mockk(relaxed = true)
    private val context: android.content.Context = mockk(relaxed = true)
    private lateinit var viewModel: SessionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SessionViewModel(
            factory,
            profileRepository,
            networkMonitor,
            mcpServerEngine,
            relayClient,
            auditJournal,
            knownHostsStore,
            context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsDisconnected() {
        val state = viewModel.uiState.value
        assertEquals(ConnectionStatus.Disconnected, state.connectionStatus)
        assertEquals("127.0.0.1", state.host)
        assertEquals("Administrator", state.username)
    }

    @Test
    fun updateHostUpdatesState() {
        viewModel.updateHost("192.168.1.100")
        assertEquals("192.168.1.100", viewModel.uiState.value.host)
    }

    @Test
    fun updateUsernameUpdatesState() {
        viewModel.updateUsername("agent-dev")
        assertEquals("agent-dev", viewModel.uiState.value.username)
    }

    @Test
    fun shareTargetPromptReceptionAndClear() {
        val sharedContent = "Check tender calculation: https://dpik.internal/tender/42"
        viewModel.setPendingSharedPrompt(sharedContent)
        assertEquals(sharedContent, viewModel.uiState.value.pendingSharedPrompt)

        viewModel.clearSharedPrompt()
        assertEquals(null, viewModel.uiState.value.pendingSharedPrompt)
    }

    @Test
    fun modalAndHudTogglesWorkCorrectly() {
        assertEquals(false, viewModel.uiState.value.showJoypad)
        viewModel.toggleJoypad()
        assertEquals(true, viewModel.uiState.value.showJoypad)
        viewModel.toggleJoypad()
        assertEquals(false, viewModel.uiState.value.showJoypad)

        viewModel.toggleTmuxPicker(true)
        assertEquals(true, viewModel.uiState.value.showTmuxPicker)
        viewModel.toggleTmuxPicker(false)
        assertEquals(false, viewModel.uiState.value.showTmuxPicker)

        viewModel.toggleMacrosModal(true)
        assertEquals(true, viewModel.uiState.value.showMacrosModal)
        viewModel.toggleMacrosModal(false)
        assertEquals(false, viewModel.uiState.value.showMacrosModal)

        viewModel.setViewMode(ViewMode.McpBridge)
        assertEquals(ViewMode.McpBridge, viewModel.uiState.value.viewMode)
        viewModel.setViewMode(ViewMode.AgentChat)
        assertEquals(ViewMode.AgentChat, viewModel.uiState.value.viewMode)
    }

    @Test
    fun saveAndSelectProfileUpdatesState() {
        viewModel.updateHost("10.0.0.42")
        viewModel.updateUsername("operator")
        viewModel.saveCurrentAsProfile("Office DevBox")
        testDispatcher.scheduler.advanceUntilIdle()

        val profiles = profileRepository.profiles.value
        val saved = profiles.find { it.name == "Office DevBox" }
        org.junit.Assert.assertNotNull(saved)
        assertEquals("10.0.0.42", saved?.host)
        assertEquals("operator", saved?.username)

        // Switch to a new host, then re-select profile
        viewModel.updateHost("8.8.8.8")
        viewModel.selectProfile(saved!!.id)
        assertEquals("10.0.0.42", viewModel.uiState.value.host)
        assertEquals("operator", viewModel.uiState.value.username)
    }

    @Test
    fun presetWorkflowMacrosIntegrity() {
        val macros = com.arh.terminal.ui.components.PRESET_WORKFLOW_MACROS
        assertEquals(6, macros.size)
        val expectedIds = setOf("urus-fleet", "dpik-quality-gate", "dpik-seam-test", "board-priorities", "dmr-search", "git-diff-stat")
        assertEquals(expectedIds, macros.map { it.id }.toSet())
        for (m in macros) {
            org.junit.Assert.assertTrue(m.title.isNotBlank())
            org.junit.Assert.assertTrue(m.description.isNotBlank())
            org.junit.Assert.assertTrue(m.command.isNotBlank())
        }
    }

    @Test
    fun approvalHudCommandRejectionAndAcceptance() {
        viewModel.rejectCommand()
        assertEquals(null, viewModel.uiState.value.pendingApprovalCommand)

        viewModel.approveCommand()
        assertEquals(null, viewModel.uiState.value.pendingApprovalCommand)
    }
}
