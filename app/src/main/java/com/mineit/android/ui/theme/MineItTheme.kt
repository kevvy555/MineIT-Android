package com.mineit.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mineit.android.ui.design.MineItPalette

private val MineItColors = darkColorScheme(
    primary = MineItPalette.Accent,
    onPrimary = Color(0xFF061018),
    secondary = MineItPalette.Success,
    onSecondary = Color(0xFF07140C),
    tertiary = MineItPalette.Warning,
    background = MineItPalette.Background,
    surface = MineItPalette.Panel,
    surfaceVariant = MineItPalette.RaisedPanel,
    outline = MineItPalette.Line,
    onBackground = MineItPalette.Text,
    onSurface = MineItPalette.Text,
    onSurfaceVariant = MineItPalette.Muted,
    error = MineItPalette.Critical,
)

private val MineItTypography = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 22.sp, fontWeight = FontWeight.Black),
    titleMedium = TextStyle(fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold),
    bodyMedium = TextStyle(fontSize = 11.sp, lineHeight = 14.sp),
    bodySmall = TextStyle(fontSize = 9.sp, lineHeight = 12.sp),
    labelLarge = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontSize = 8.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold),
    labelSmall = TextStyle(fontSize = 7.sp, lineHeight = 9.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun MineItTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MineItColors,
        typography = MineItTypography,
        content = content,
    )
}
