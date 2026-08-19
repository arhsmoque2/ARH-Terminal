package com.arh.terminal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MacroAction(
    val id: String,
    val title: String,
    val description: String,
    val command: String
)

val PRESET_WORKFLOW_MACROS = listOf(
    MacroAction(
        id = "urus-fleet",
        title = "🚀 Spawn URUS 4-Agent Fleet",
        description = "Spawns 4-pane psmux workstation (Triage + MCP + Kanban + G4)",
        command = "pwsh -File D:/ARH-GITHUB/arhsmoque2/ARH-URUS-Work/scripts/psmux-agent-fleet.ps1 -SessionName urus-fleet"
    ),
    MacroAction(
        id = "dpik-quality-gate",
        title = "📋 DPIK Tender Quality Gate",
        description = "Runs InvokeBuild on tender calculation toolkit",
        command = "Invoke-Build -File D:/ARH-DPIK/tender-devkit.build.ps1 -Task QualityGate"
    ),
    MacroAction(
        id = "dpik-seam-test",
        title = "📄 DPIK Contract Seam Test",
        description = "Executes WP2 contract Pester tests",
        command = "Invoke-Pester -Path D:/ARH-DPIK/tests/contract-seams.tests.ps1"
    ),
    MacroAction(
        id = "board-priorities",
        title = "📊 Board Active Priorities",
        description = "Queries agent-board CLI for live priorities",
        command = "python _AGENT-WORKSPACE/agent-board/core/board_cli.py view"
    ),
    MacroAction(
        id = "dmr-search",
        title = "🔍 DMR Technical Fix Search",
        description = "Searches debug maintenance records",
        command = "uv run dmr_cli.py search \"psmux\""
    ),
    MacroAction(
        id = "git-diff-stat",
        title = "🌿 Git Status & Diff Summary",
        description = "Fast audit of current working tree",
        command = "git status -s; git diff --stat"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowMacrosModal(
    onTriggerMacro: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12121A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "⚡ 1-Tap Workflow & Fleet Macros",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Trigger DevBox workflows, URUS fleet, or tender gates on the fly.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(PRESET_WORKFLOW_MACROS) { macro ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTriggerMacro(macro.command) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = macro.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "FIRE ⚡",
                                    color = Color(0xFF61AFEF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = macro.description,
                                color = Color(0xFFABB2BF),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = macro.command,
                                color = Color(0xFF98C379),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
