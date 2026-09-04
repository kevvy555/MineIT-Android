package com.mineit.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MineItColors = darkColorScheme(
    primary = Color(0xFFE8B455),
    onPrimary = Color(0xFF231A0C),
    secondary = Color(0xFF74C69D),
    background = Color(0xFF090B10),
    surface = Color(0xFF121620),
    surfaceVariant = Color(0xFF1A2030),
    onBackground = Color(0xFFF3F5F7),
    onSurface = Color(0xFFF3F5F7),
)

@Composable
fun MineItTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MineItColors,
        content = content,
    )
}
