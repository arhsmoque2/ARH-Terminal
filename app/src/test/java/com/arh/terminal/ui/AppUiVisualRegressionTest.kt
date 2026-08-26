package com.arh.terminal.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.arh.terminal.core.mcp.server.McpServerEngine
import com.arh.terminal.core.mcp.server.McpServerStats
import com.arh.terminal.core.relay.client.RelayStatus
import com.arh.terminal.core.relay.client.RelayWebSocketClient
import com.arh.terminal.data.audit.AgentAuditJournal
import com.arh.terminal.data.profiles.ProfileRepository
import com.arh.terminal.data.security.KnownHostsStore
import com.arh.terminal.ui.components.WorkflowMacrosModal
import com.arh.terminal.ui.session.SessionScreen
import com.arh.terminal.ui.session.SessionViewModel
import com.arh.terminal.ui.theme.ARHTerminalTheme
import com.arh.terminal.util.NetworkMonitor
import com.arh.terminal.util.NetworkStatus
import com.arh.terminal.util.NetworkType
import com.github.takahirom.roborazzi.captureRoboImage
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tier 2 of the UI quality gate: pixel-level screenshot regression, on top of the
 * semantics-only assertions in [AppUiQualityGateTest]. Robolectric Native Graphics (RNG)
 * via Roborazzi renders these composables headlessly — no emulator, no accessibility-tree
 * mapping, none of the failure modes the retired `.maestro/` flows hit.
 *
 * IMPORTANT — no baseline goldens are committed yet. Without `-Proborazzi.test.record=true`
 * or `-Proborazzi.test.verify=true`, these tests only *capture* images to
 * `app/build/outputs/roborazzi/` (uploaded as a CI artifact) — they do not fail on visual
 * change yet. To turn this into a real regression gate:
 *   1. Run `./gradlew :app:recordRoborazziDebug` locally (or CI, once) to record baselines.
 *   2. Commit the resulting PNGs under `app/src/test/screenshots/`.
 *   3. Add `-Proborazzi.test.verify=true` to the CI unit-test step so future runs compare
 *      against those goldens and fail on unintended visual drift.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-420dpi")
@OptIn(ExperimentalCoroutinesApi::class)
class AppUiVisualRegressionTest {

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
        RuntimeEnvironment.setFontScale(1.0f)
        Dispatchers.resetMain()
    }

    @Test
    fun captureSessionScreenDefaultState() {
        composeTestRule.setContent {
            ARHTerminalTheme {
                SessionScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureSessionScreenAtFontScale200() {
        RuntimeEnvironment.setFontScale(2.0f)

        composeTestRule.setContent {
            ARHTerminalTheme {
                SessionScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureWorkflowMacrosModal() {
        composeTestRule.setContent {
            ARHTerminalTheme {
                WorkflowMacrosModal(
                    onTriggerMacro = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }
}
