package com.arh.terminal.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arh.terminal.data.github.GitHubAuthManager
import com.arh.terminal.data.github.GitHubClient
import com.arh.terminal.data.github.GitHubDeviceAuth
import com.arh.terminal.data.github.GitHubRepo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoPickerBottomSheet(
    authManager: GitHubAuthManager,
    client: GitHubClient,
    onDismiss: () -> Unit,
    onSelectRepo: (GitHubRepo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isAuthorized by remember { mutableStateOf(authManager.isAuthorized()) }
    var deviceAuth by remember { mutableStateOf<GitHubDeviceAuth?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    var isLoadingRepos by remember { mutableStateOf(false) }
    var repos by remember { mutableStateOf<List<GitHubRepo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var repoError by remember { mutableStateOf<String?>(null) }

    fun loadRepos() {
        if (!isAuthorized) return
        isLoadingRepos = true
        repoError = null
        scope.launch {
            val res = client.listUserRepositories()
            isLoadingRepos = false
            res.onSuccess {
                repos = it
            }.onFailure {
                repoError = it.message ?: "Failed to load repositories"
            }
        }
    }

    LaunchedEffect(isAuthorized) {
        if (isAuthorized) {
            loadRepos()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16161A),
        contentColor = Color(0xFFE0E0E0),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3E3E4A))
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(bottom = 16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GitHub Repositories",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAuthorized) {
                        IconButton(onClick = { loadRepos() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF9E9E9E))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF9E9E9E))
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF2C2C35))

            if (!isAuthorized) {
                // Device Flow Auth View
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF24292E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connect GitHub Account",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Bind remote repositories to your ARH-Terminal SSH workspaces without manual token creation.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF9E9E9E)
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (deviceAuth != null) {
                        val auth = deviceAuth!!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E24))
                                .border(1.dp, Color(0xFF38384A), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "ONE-TIME CODE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF888888)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = auth.userCode,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("GitHub Code", auth.userCode))
                                        Toast.makeText(context, "Code copied: ${auth.userCode}", Toast.LENGTH_SHORT).show()
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(auth.verificationUri))
                                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(browserIntent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Copy Code & Open GitHub")
                                }

                                if (isAuthenticating) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Waiting for authorization on browser...", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                isAuthenticating = true
                                authError = null
                                scope.launch {
                                    val codeResult = authManager.requestDeviceCode()
                                    codeResult.onSuccess { auth ->
                                        deviceAuth = auth
                                        val pollResult = authManager.pollForAccessToken(auth.deviceCode, auth.interval)
                                        isAuthenticating = false
                                        pollResult.onSuccess {
                                            isAuthorized = true
                                            Toast.makeText(context, "GitHub Account Connected!", Toast.LENGTH_SHORT).show()
                                        }.onFailure {
                                            authError = it.message ?: "Authentication timed out"
                                        }
                                    }.onFailure {
                                        isAuthenticating = false
                                        authError = it.message ?: "Failed to initiate device flow"
                                    }
                                }
                            },
                            enabled = !isAuthenticating,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Requesting Code...")
                            } else {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign In with GitHub (Device Flow)")
                            }
                        }
                    }

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = authError!!,
                            color = Color(0xFFFF8A80),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                // Repositories List View
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter repositories...", color = Color(0xFF757575), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF9E9E9E)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF9E9E9E))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF38384A),
                            focusedContainerColor = Color(0xFF1E1E24),
                            unfocusedContainerColor = Color(0xFF1E1E24),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoadingRepos) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (repoError != null) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(text = repoError!!, color = Color(0xFFFF8A80), fontSize = 13.sp)
                        }
                    } else {
                        val filteredRepos = repos.filter {
                            searchQuery.isBlank() || it.fullName.contains(searchQuery, ignoreCase = true)
                        }

                        if (filteredRepos.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                Text("No repositories found.", color = Color(0xFF757575), fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(filteredRepos, key = { it.id }) { repo ->
                                    RepoItemCard(
                                        repo = repo,
                                        onSelect = {
                                            onSelectRepo(repo)
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepoItemCard(
    repo: GitHubRepo,
    onSelect: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF1E1E24))
            .border(1.dp, Color(0xFF2C2C38), shape)
            .clickable(onClick = onSelect)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (repo.isPrivate) Icons.Default.Lock else Icons.Default.Public,
                    contentDescription = null,
                    tint = if (repo.isPrivate) Color(0xFFFFB74D) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = repo.fullName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    ),
                    maxLines = 1
                )
            }
            if (!repo.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = repo.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF9E9E9E)
                    ),
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "branch: ${repo.defaultBranch}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF757575)
                )
                if (repo.starsCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = "${repo.starsCount}", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                }
            }
        }

        IconButton(onClick = onSelect) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Select Repository",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
