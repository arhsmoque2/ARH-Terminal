package com.arh.terminal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Mobile-first Gamepad Joypad Bar (inspired by moggsh).
 * Allows navigating vim, tmux windows, fzf, and history without opening the soft keyboard.
 */
@Composable
fun GamepadJoypadBar(
    onSendKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gamepad_joypad_bar"),
        color = Color(0xFF16161E),
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left D-Pad cluster
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DPadButton(icon = Icons.Filled.KeyboardArrowLeft, label = "Left") {
                    onSendKey("\u001B[D")
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DPadButton(icon = Icons.Filled.KeyboardArrowUp, label = "Up") {
                        onSendKey("\u001B[A")
                    }
                    DPadButton(icon = Icons.Filled.KeyboardArrowDown, label = "Down") {
                        onSendKey("\u001B[B")
                    }
                }
                DPadButton(icon = Icons.Filled.KeyboardArrowRight, label = "Right") {
                    onSendKey("\u001B[C")
                }
            }

            // Right Action Clusters
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                JoyActionButton(label = "ESC", color = Color(0xFFE06C75)) {
                    onSendKey("\u001B")
                }
                JoyActionButton(label = "TAB", color = Color(0xFF61AFEF)) {
                    onSendKey("\t")
                }
                JoyActionButton(label = "^C", color = Color(0xFFE5C07B)) {
                    onSendKey("\u0003")
                }
                JoyActionButton(label = "ENTER", color = Color(0xFF98C379)) {
                    onSendKey("\r")
                }
            }
        }
    }
}

@Composable
private fun DPadButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val tag = "dpad_${label.lowercase()}"
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF22222E))
            .clickable { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFFABB2BF),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun JoyActionButton(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val sanitizedTag = "joy_btn_${label.lowercase().replace("^", "ctrl_")}"
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF22222E))
            .clickable { onClick() }
            .testTag(sanitizedTag)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
