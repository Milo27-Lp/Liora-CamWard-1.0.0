package com.aistudio.lioracamward.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RadarNeonGreen,
    onPrimary = Color(0xFF041A10),
    primaryContainer = RadarGreenDark,
    onPrimaryContainer = RadarNeonGreen,
    secondary = RadarCyan,
    onSecondary = Color(0xFF031B24),
    secondaryContainer = Color(0xFF0C2B38),
    onSecondaryContainer = RadarCyan,
    tertiary = WarningAmber,
    background = CyberBackground,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberCard,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardBorder,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun LioraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
