package com.arh.terminal.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.arh.terminal.core.mcp.server.McpServerEngine
import com.arh.terminal.core.mcp.server.McpServerStats
import com.arh.terminal.core.relay.client.RelayStatus
import com.arh.terminal.core.relay.client.RelayWebSocketClient
import com.arh.terminal.data.audit.AgentAuditJournal
import com.arh.terminal.data.profiles.ProfileRepository
import com.arh.terminal.data.security.KnownHostsStore
import com.arh.terminal.ui.components.GamepadJoypadBar
import com.arh.terminal.ui.components.QuickActionBar
import com.arh.terminal.ui.components.WorkflowMacrosModal
import com.arh.terminal.ui.session.SessionScreen
import com.arh.terminal.ui.session.SessionViewModel
import com.arh.terminal.ui.theme.ARHTerminalTheme
import com.arh.terminal.util.NetworkMonitor
import com.arh.terminal.util.NetworkStatus
import com.arh.terminal.util.NetworkType
import com.pocketshell.core.tmux.TmuxClientFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-420dpi")
@OptIn(ExperimentalCoroutinesApi::class)
class AppUiQualityGateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private val factory: TmuxClientFactory = mockk(relaxed = true)
    private val profileRepository: ProfileRepository = ProfileRepository()
    private val networkMonitor: NetworkMonitor = mockk(relaxed = true)
    private val mcpServerEngine: McpServerEngine = mockk(relaxed = true)
    private val relayClient: RelayWebSocketClient = mockk(relaxed = true)
    private val auditJournal: AgentAuditJournal = AgentAuditJournal()
    private val knownHostsStore: KnownHostsStore = mockk(relaxed = true)
    private val gitHubAuthManager: com.arh.terminal.data.github.GitHubAuthManager = mockk(relaxed = true)
    private val gitHubClient: com.arh.terminal.data.github.GitHubClient = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private lateinit var viewModel: SessionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { networkMonitor.status } returns MutableStateFlow(NetworkStatus(true, NetworkType.Wifi))
        every { relayClient.status } returns MutableStateFlow(RelayStatus.Disconnected)
        every { mcpServerEngine.stats } returns MutableStateFlow(McpServerStats())

        viewModel = SessionViewModel(
            factory,
            profileRepository,
            networkMonitor,
            mcpServerEngine,
            relayClient,
            auditJournal,
            knownHostsStore,
            gitHubAuthManager,
            gitHubClient,
            context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun verifyAppLaunchAndHeaderComponents() {
        composeTestRule.setContent {
            ARHTerminalTheme {
                SessionScreen(viewModel = viewModel)
            }
        }

        // Verify App Header & Title
        composeTestRule.onNodeWithText("ARH Terminal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("app_title").assertIsDisplayed()

        // Verify Connection Input Form Fields exist in composition
        assertNotNull(composeTestRule.onNodeWithTag("input_host").fetchSemanticsNode())
        assertNotNull(composeTestRule.onNodeWithTag("input_username").fetchSemanticsNode())
        assertNotNull(composeTestRule.onNodeWithTag("input_password").fetchSemanticsNode())
        assertNotNull(composeTestRule.onNodeWithTag("btn_connect").fetchSemanticsNode())

        // Verify Network Status Badge & MCP Switch
        composeTestRule.onNodeWithTag("network_status_badge").assertIsDisplayed()
        assertNotNull(composeTestRule.onNodeWithTag("switch_mcp_daemon").fetchSemanticsNode())
    }

    @Test
    fun verifyWorkflowMacrosModalRender() {
        composeTestRule.setContent {
            ARHTerminalTheme {
                WorkflowMacrosModal(
                    onTriggerMacro = {},
                    onDismiss = {}
                )
            }
        }

        // Assert Modal content is composed and visible
        assertNotNull(composeTestRule.onNodeWithTag("modal_workflow_macros", useUnmergedTree = true).fetchSemanticsNode())
        assertNotNull(composeTestRule.onNodeWithText("⚡ 1-Tap Workflow & Fleet Macros", useUnmergedTree = true).fetchSemanticsNode())
    }

    @Test
    fun verifyFontScale200AccessibilityAndLayoutResilience() {
        // Apply 2.0x font scaling
        RuntimeEnvironment.setFontScale(2.0f)

        composeTestRule.setContent {
            ARHTerminalTheme {
                SessionScreen(viewModel = viewModel)
            }
        }

        // Assert core controls remain composed and functional under 200% dynamic font scale
        composeTestRule.onNodeWithText("ARH Terminal").assertIsDisplayed()
        assertNotNull(composeTestRule.onNodeWithTag("input_host").fetchSemanticsNode())
        assertNotNull(composeTestRule.onNodeWithTag("btn_connect").fetchSemanticsNode())
    }

    // --- Parity with the retired .maestro/03_hud_and_joypad_clash_audit.yaml flow ---

    @Test
    fun verifyNoHudApprovalCardOnDefaultState() {
        composeTestRule.setContent {
            ARHTerminalTheme {
                SessionScreen(viewModel = viewModel)
            }
        }

        // The Floating Approval HUD must not render when there is no pending command —
        // it should never collide with the setup form or nav on a fresh/disconnected screen.
        assertTrue(
            "hud_approval_card must not be present with no pending approval command",
            composeTestRule.onAllNodesWithTag("hud_approval_card").fetchSemanticsNodes(atLeastOneRootRequired = false).isEmpty()
        )
    }

    @Test
    fun verifyJoypadAndQuickActionBarDoNotVisuallyClash() {
        // Mirrors how SessionScreen stacks these two floating bars when attached to a
        // session with the joypad toggled on (GamepadJoypadBar above QuickActionBar).
        composeTestRule.setContent {
            ARHTerminalTheme {
                Column {
                    GamepadJoypadBar(onSendKey = {})
                    QuickActionBar(onSendKey = {})
                }
            }
        }

        val joypadBounds = composeTestRule.onNodeWithTag("gamepad_joypad_bar").fetchSemanticsNode().boundsInRoot
        val quickActionBounds = composeTestRule.onNodeWithTag("quick_action_rail").fetchSemanticsNode().boundsInRoot

        assertTrue("GamepadJoypadBar must have a laid-out height", joypadBounds.height > 0f)
        assertTrue("QuickActionBar must have a laid-out height", quickActionBounds.height > 0f)
        assertFalse(
            "GamepadJoypadBar and QuickActionBar must not vertically overlap",
            joypadBounds.bottom > quickActionBounds.top && quickActionBounds.bottom > joypadBounds.top
        )
    }
}
