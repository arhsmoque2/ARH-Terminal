package com.arh.terminal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MacroAction(
    val title: String,
    val description: String,
    val command: String,
    val icon: ImageVector,
    val category: String
)

val PRESET_WORKFLOW_MACROS = listOf(
    // 📋 DPIK Tender Workflows
    MacroAction(
        title = "DPIK Tender Quality Gate",
        description = "Runs InvokeBuild quality gate on DPIK Tender toolkit",
        command = "Set-Location 'D:\\ARH-DPIK\\0-DPIK-TENDER\\_tools'; Invoke-Build -Task QualityGate",
        icon = Icons.Default.Assessment,
        category = "DPIK Tender"
    ),
    MacroAction(
        title = "DPIK Contract Seam Test",
        description = "Executes DPIK WP2 contract test suite",
        command = "Set-Location 'D:\\ARH-DPIK\\0-DPIK-TENDER\\_tools'; Invoke-Pester -Path ./tests/contracts",
        icon = Icons.Default.DataObject,
        category = "DPIK Tender"
    ),
    // 📊 Agent Board & Memory
    MacroAction(
        title = "Board: Show Active Priorities",
        description = "Inspect current flagged cross-agent projects",
        command = "python D:\\_ARH-AGENT-OS\\_AGENT-WORKSPACE\\agent-board\\core\\board_cli.py view",
        icon = Icons.Default.Dashboard,
        category = "ARH OS"
    ),
    MacroAction(
        title = "DMR: Search Technical Fix History",
        description = "Search Debug Maintenance Record database",
        command = "uv run python D:\\_ARH-AGENT-OS\\_AGENT-MEMORY-KNOWLEDGE-HUB\\arh-local-knowledge-db\\arh-agent-evidence-log\\dmr_cli.py list",
        icon = Icons.Default.BugReport,
        category = "ARH OS"
    ),
    // ⚡ Fast Git & Discovery
    MacroAction(
        title = "Git Status & Working Tree Diff",
        description = "Quick summary of modified and staged files",
        command = "git status -s; git diff --stat",
        icon = Icons.Default.Code,
        category = "Developer"
    ),
    MacroAction(
        title = "ES Discovery: Everything 1.5a Root",
        description = "Resolve canonical registry root instantly",
        command = "es.exe -instance 1.5a find \$env:ARH_PATH_REGISTRY_ROOT",
        icon = Icons.Default.FindInPage,
        category = "Developer"
    )
)

@Composable
fun WorkflowMacrosModal(
    onSelectMacro: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1-Tap Workflow Macros",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            LazyColumn(
                modifier = Modifier.height(280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PRESET_WORKFLOW_MACROS) { macro ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                            .clickable { onSelectMacro(macro.command) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(macro.icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = macro.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "[${macro.category}]",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                text = macro.description,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
