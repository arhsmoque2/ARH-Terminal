package com.arh.terminal.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arh.terminal.ui.conversation.AgentTurnCard

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
                    onSend = viewModel::sendPrompt,
                    onApprove = viewModel::approvePrompt,
                    onViewModeChange = viewModel::setViewMode
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
                label = { Text("Password (or SSH Key in ~/.ssh)") },
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
    onSend: (String) -> Unit,
    onApprove: (Boolean) -> Unit,
    onViewModeChange: (ViewMode) -> Unit
) {
    var inputPrompt by remember { mutableStateOf("") }
    val activePane = state.panes.find { it.paneId == state.selectedPaneId } ?: state.panes.firstOrNull()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
            if (state.viewMode == ViewMode.AgentChat) {
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

        // Send Prompt Input Row
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
