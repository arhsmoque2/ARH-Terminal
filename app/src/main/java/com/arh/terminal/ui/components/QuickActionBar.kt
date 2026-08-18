package com.arh.terminal.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class QuickKey(val label: String, val rawSequence: String, val isCommand: Boolean = false) {
    data object Esc : QuickKey("ESC", "\u001b")
    data object Tab : QuickKey("TAB", "\t")
    data object CtrlC : QuickKey("^C", "\u0003")
    data object CtrlD : QuickKey("^D", "\u0004")
    data object Up : QuickKey("▲", "\u001b[A")
    data object Down : QuickKey("▼", "\u001b[B")
    data object Compact : QuickKey("/compact", "/compact\n", isCommand = true)
    data object Clear : QuickKey("/clear", "/clear\n", isCommand = true)
    data object Review : QuickKey("/review", "/review\n", isCommand = true)
}

@Composable
fun QuickActionBar(
    onSendKey: (QuickKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        QuickKey.Esc,
        QuickKey.Tab,
        QuickKey.CtrlC,
        QuickKey.CtrlD,
        QuickKey.Up,
        QuickKey.Down,
        QuickKey.Compact,
        QuickKey.Clear,
        QuickKey.Review
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF141414),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            keys.forEach { key ->
                OutlinedButton(
                    onClick = { onSendKey(key) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (key.isCommand) Color(0xFF1E293B) else Color(0xFF262626),
                        contentColor = if (key.isCommand) Color(0xFF38BDF8) else Color(0xFFE2E8F0)
                    )
                ) {
                    Text(
                        text = key.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
