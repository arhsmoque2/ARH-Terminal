package com.arh.terminal.ui.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arh.terminal.core.mcp.server.McpServerStats
import com.arh.terminal.data.profiles.ConnectionProfile
import com.arh.terminal.ui.components.FloatingApprovalHud
import com.arh.terminal.ui.components.WorkflowMacrosModal
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentPaste
import com.arh.terminal.ui.components.QuickActionBar
import com.arh.terminal.ui.conversation.AgentTurnCard
import com.arh.terminal.util.NetworkType

@Composable
fun SessionScreen(
    viewModel: SessionViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Terminal",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ARH Terminal",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Live Network Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (state.networkStatus.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = if (state.networkStatus.isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (state.networkStatus.isConnected) state.networkStatus.type.name else "Offline",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                IconButton(onClick = { viewModel.toggleMacrosModal(!state.showMacrosModal) }) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Workflow Macros",
                            tint = Color(0xFFFBBF24)
                        )
                    }
                    if (state.connectionStatus is ConnectionStatus.Connected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.disconnect() }) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Disconnect",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        when (val status = state.connectionStatus) {
            is ConnectionStatus.Disconnected, is ConnectionStatus.Error -> {
                ConnectionSetupCard(
                    state = state,
                    error = (status as? ConnectionStatus.Error)?.message,
                    onSelectProfile = viewModel::selectProfile,
                    onSaveProfile = viewModel::saveCurrentAsProfile,
                    onHostChange = viewModel::updateHost,
                    onUserChange = viewModel::updateUsername,
                    onConnect = { password -> viewModel.connect(password) },
                    onToggleMcp = viewModel::toggleMcpServer
                )
            }
            is ConnectionStatus.Connecting -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = status.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            is ConnectionStatus.Connected -> {
                ConnectedSessionView(
                    state = state,
                    viewModel = viewModel,
                    onSend = viewModel::sendPrompt,
                    onApprove = viewModel::approvePrompt,
                    onViewModeChange = viewModel::setViewMode,
                    onToggleAttach = viewModel::toggleAttach,
                    onSelectSession = viewModel::switchSession,
                    onCreateSession = viewModel::createNewSession,
                    onToggleMcp = viewModel::toggleMcpServer
                )
            }
        }
    }
}

@Composable
private fun ConnectionSetupCard(
    state: SessionUiState,
    error: String?,
    onSelectProfile: (ConnectionProfile) -> Unit,
    onSaveProfile: (String) -> Unit,
    onHostChange: (String) -> Unit,
    onUserChange: (String) -> Unit,
    onConnect: (String) -> Unit,
    onToggleMcp: (Boolean, Int, String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showSaveProfile by remember { mutableStateOf(false) }
    var profileNameInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Connect to Dev Machine (psmux / tmux -CC)",
                style = MaterialTheme.typography.titleMedium
            )

            // Saved Profiles Horizontal Chips
            if (state.profiles.isNotEmpty()) {
                Text(
                    text = "Saved Profiles",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.profiles.forEach { profile ->
                        FilterChip(
                            selected = profile.id == state.selectedProfileId,
                            onClick = { onSelectProfile(profile) },
                            label = { Text(profile.name) },
                            leadingIcon = {
                                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }

            if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = state.host,
                onValueChange = onHostChange,
                label = { Text("Host / IP") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.username,
                onValueChange = onUserChange,
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Private Key PEM / Passphrase") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onConnect(password) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Connect")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Connect (psmux -CC)")
                }

                OutlinedButton(
                    onClick = { showSaveProfile = !showSaveProfile }
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Save Profile")
                }
            }

            AnimatedVisibility(visible = showSaveProfile) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = profileNameInput,
                        onValueChange = { profileNameInput = it },
                        label = { Text("Profile Name (e.g. My Laptop)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (profileNameInput.isNotBlank()) {
                                onSaveProfile(profileNameInput)
                                profileNameInput = ""
                                showSaveProfile = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }

            // Embedded MCP Daemon Status Tile on Setup Screen
            McpMiniTile(stats = state.mcpStats, onToggle = onToggleMcp)
        }
    }
}

@Composable
private fun McpMiniTile(
    stats: McpServerStats,
    onToggle: (Boolean, Int, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = if (stats.isRunning) Color(0xFF10B981) else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Android MCP Agent Bridge",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (stats.isRunning) "Listening on port ${stats.port} (:8070/mcp)" else "Stopped",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (stats.isRunning) Color(0xFF38BDF8) else Color.Gray
                    )
                }
            }

            Switch(
                checked = stats.isRunning,
                onCheckedChange = { onToggle(it, 8070, "ca48ffe8-cb63-45be-bfd5-1911e367fbcd") },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF10B981),
                    checkedTrackColor = Color(0xFF065F46)
                )
            )
        }
    }
}

@Composable
private fun ConnectedSessionView(
    state: SessionUiState,
    viewModel: SessionViewModel,
    onSend: (String) -> Unit,
    onApprove: (Boolean) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onToggleAttach: (Boolean) -> Unit,
    onSelectSession: (String) -> Unit,
    onCreateSession: (String) -> Unit,
    onToggleMcp: (Boolean, Int, String) -> Unit
) {
    var inputPrompt by remember { mutableStateOf("") }
    var showNewSessionInput by remember { mutableStateOf(false) }
    var newSessionName by remember { mutableStateOf("") }
    val activePane = state.panes.find { it.paneId == state.selectedPaneId } ?: state.panes.firstOrNull()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- 🎛️ Interactive psmux Attach/Detach Slider Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (state.isAttached) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF2A2A2A)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (state.isAttached) Color(0xFF00E676) else Color(0xFF757575))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "psmux: ${state.activeSessionName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (state.isAttached) "Streaming Live (-CC attached)" else "Parked on PC (Detached)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.isAttached) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }

                    Switch(
                        checked = state.isAttached,
                        onCheckedChange = { onToggleAttach(it) },
                        thumbContent = {
                            Icon(
                                imageVector = if (state.isAttached) Icons.Default.Link else Icons.Default.LinkOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.availableSessions.forEach { sessionName ->
                        FilterChip(
                            selected = sessionName == state.activeSessionName,
                            onClick = { onSelectSession(sessionName) },
                            label = { Text(sessionName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }

                    IconButton(
                        onClick = { showNewSessionInput = !showNewSessionInput },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Session",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                AnimatedVisibility(visible = showNewSessionInput) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSessionName,
                            onValueChange = { newSessionName = it },
                            label = { Text("New psmux session name...") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newSessionName.isNotBlank()) {
                                    onCreateSession(newSessionName)
                                    newSessionName = ""
                                    showNewSessionInput = false
                                }
                            }
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }

        // View Mode Switcher: Agent Chat vs Raw Terminal vs MCP Bridge
        TabRow(
            selectedTabIndex = when (state.viewMode) {
                ViewMode.AgentChat -> 0
                ViewMode.RawTerminal -> 1
                ViewMode.McpBridge -> 2
            }
        ) {
            Tab(
                selected = state.viewMode == ViewMode.AgentChat,
                onClick = { onViewModeChange(ViewMode.AgentChat) },
                text = { Text("Agent Chat") },
                icon = { Icon(Icons.Default.Chat, contentDescription = null) }
            )
            Tab(
                selected = state.viewMode == ViewMode.RawTerminal,
                onClick = { onViewModeChange(ViewMode.RawTerminal) },
                text = { Text("Terminal Feed") },
                icon = { Icon(Icons.Default.Terminal, contentDescription = null) }
            )
            Tab(
                selected = state.viewMode == ViewMode.McpBridge,
                onClick = { onViewModeChange(ViewMode.McpBridge) },
                text = { Text("MCP Bridge") },
                icon = { Icon(Icons.Default.Sensors, contentDescription = null) }
            )
        }

        // View Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (state.viewMode == ViewMode.McpBridge) {
                McpBridgeDashboard(stats = state.mcpStats, onToggle = onToggleMcp)
            } else if (!state.isAttached) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LinkOff,
                            contentDescription = "Detached",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Session is detached and running on PC.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { onToggleAttach(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Attach & Resume Stream")
                        }
                    }
                }
            } else if (state.viewMode == ViewMode.AgentChat) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activePane?.agentTurns.isNullOrEmpty()) {
                        item {
                            Text(
                                text = "Waiting for agent activity in ${state.activeSessionName}...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(activePane.agentTurns) { turn ->
                            AgentTurnCard(turn = turn, onApproveTool = { _, approve -> onApprove(approve) })
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        items(activePane?.outputHistory ?: emptyList()) { chunk ->
                            Text(
                                text = chunk,
                                color = Color(0xFF80D8FF),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // --- 🌟 1-Tap DPIK & ARH Workflow Macros Drawer ---
        if (state.showMacrosModal) {
            WorkflowMacrosModal(
                onSelectMacro = { cmd ->
                    viewModel.sendPrompt(cmd)
                    viewModel.toggleMacrosModal(false)
                },
                onDismiss = { viewModel.toggleMacrosModal(false) }
            )
        }

        // --- ⚡ Moggsh-style Floating Approval HUD ---
        if (state.viewMode != ViewMode.McpBridge) {
            FloatingApprovalHud(
                pendingCommand = state.pendingApprovalCommand,
                onApprove = onApprove
            )
        }

        // --- ⌨️ Quick-Action Extended Key Bar ---
        if (state.isAttached && state.viewMode != ViewMode.McpBridge) {
            QuickActionBar(
                onSendKey = { key -> viewModel.sendRawKey(key.rawSequence) }
            )
        }

        // Send Prompt Input Row
        if (state.isAttached && state.viewMode != ViewMode.McpBridge) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    label = { Text("Type prompt or /slash command...") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputPrompt.isNotBlank()) {
                            onSend(inputPrompt)
                            inputPrompt = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun McpBridgeDashboard(
    stats: McpServerStats,
    onToggle: (Boolean, Int, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeveloperBoard,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "On-Device MCP Agent Server",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "56 Native Android Tools Exported",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Switch(
                    checked = stats.isRunning,
                    onCheckedChange = { onToggle(it, 8070, "ca48ffe8-cb63-45be-bfd5-1911e367fbcd") },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF10B981),
                        checkedTrackColor = Color(0xFF065F46)
                    )
                )
            }

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Total Tool Calls", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "${stats.totalToolCalls}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Active Sessions", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "${stats.activeSessions}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                }
            }

            // Connection Info Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "PC Agent Endpoint", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    Text(
                        text = "http://100.85.170.170:${stats.port}/mcp",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFFFBBF24)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Bearer Secret Token", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    Text(
                        text = stats.bearerToken,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            if (stats.lastToolCall != null) {
                Text(
                    text = "Last executed tool: ${stats.lastToolCall}",
                    fontSize = 12.sp,
                    color = Color(0xFF10B981),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
