package com.arh.terminal.ui.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.arh.terminal.ui.conversation.AgentTurnCard
import com.arh.terminal.ui.components.FloatingApprovalHud
import com.arh.terminal.ui.components.QuickActionBar
import com.arh.terminal.ui.components.QuickKey

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

            if (state.connectionStatus is ConnectionStatus.Connected) {
                IconButton(onClick = { viewModel.disconnect() }) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Disconnect",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        when (val status = state.connectionStatus) {
            is ConnectionStatus.Disconnected, is ConnectionStatus.Error -> {
                ConnectionSetupCard(
                    state = state,
                    error = (status as? ConnectionStatus.Error)?.message,
                    onHostChange = viewModel::updateHost,
                    onUserChange = viewModel::updateUsername,
                    onConnect = { password -> viewModel.connect(password) }
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
                    onCreateSession = viewModel::createNewSession
                )
            }
        }
    }
}

@Composable
private fun ConnectionSetupCard(
    state: SessionUiState,
    error: String?,
    onHostChange: (String) -> Unit,
    onUserChange: (String) -> Unit,
    onConnect: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

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

            Button(
                onClick = { onConnect(password) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Connect")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect (psmux -CC)")
            }
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
    onCreateSession: (String) -> Unit
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
                        // Live pulse indicator (Green when attached, Grey when parked)
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

                    // Attach / Detach Toggle Slider Switch
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

                // Session Switcher Horizontal Chips
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

        // View Mode Switcher: Agent Chat vs Raw Terminal
        TabRow(
            selectedTabIndex = if (state.viewMode == ViewMode.AgentChat) 0 else 1
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
        }

        // View Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (!state.isAttached) {
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

        // --- ⚡ Moggsh-style Floating Approval HUD ---
        FloatingApprovalHud(
            pendingCommand = state.pendingApprovalCommand,
            onApprove = onApprove
        )

        // --- ⌨️ Quick-Action Extended Key Bar ---
        if (state.isAttached) {
            QuickActionBar(
                onSendKey = { key -> viewModel.sendRawKey(key.rawSequence) }
            )
        }

        // Send Prompt Input Row
        if (state.isAttached) {
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
