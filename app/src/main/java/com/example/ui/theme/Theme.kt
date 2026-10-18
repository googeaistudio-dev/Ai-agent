package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF041824),
    primaryContainer = Color(0xFF06334D),
    onPrimaryContainer = NeonCyanLight,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1A47),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = NeonMagenta,
    onTertiary = Color.White,
    background = CyberBackground,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Color(0x3300E5FF),
    error = AlertRed,
    onError = Color.White
)

private val LightColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF041824),
    primaryContainer = Color(0xFF06334D),
    onPrimaryContainer = NeonCyanLight,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1A47),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = NeonMagenta,
    onTertiary = Color.White,
    background = CyberBackground,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Color(0x3300E5FF),
    error = AlertRed,
    onError = Color.White
)

@Composable
fun NafsiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We intentionally maintain a cohesive sci-fi neon theme in both modes
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Keep backward-compatible alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    NafsiTheme(darkTheme = darkTheme, content = content)
}
