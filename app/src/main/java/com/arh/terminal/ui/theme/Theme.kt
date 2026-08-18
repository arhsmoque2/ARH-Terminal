package com.arh.terminal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80D8FF),
    secondary = Color(0xFF82B1FF),
    tertiary = Color(0xFFA7FFEB),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    onPrimary = Color(0xFF00363A),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006978),
    secondary = Color(0xFF006494),
    tertiary = Color(0xFF006A60),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFEFEF)
)

@Composable
fun ARHTerminalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
