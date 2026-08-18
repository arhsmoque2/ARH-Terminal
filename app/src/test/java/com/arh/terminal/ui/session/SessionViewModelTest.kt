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
    private lateinit var viewModel: SessionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SessionViewModel(factory, profileRepository, networkMonitor, mcpServerEngine, relayClient)
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
}
