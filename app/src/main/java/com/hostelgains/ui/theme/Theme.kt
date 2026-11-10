package com.hostelgains.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Volt,
    onPrimary = Ink,
    primaryContainer = Panel,
    onPrimaryContainer = Volt,
    secondary = Ember,
    onSecondary = Color.White,
    secondaryContainer = Surface,
    onSecondaryContainer = Ember,
    tertiary = CyanAccent,
    onTertiary = Ink,
    background = Ink,
    onBackground = TextPrimary,
    surface = Panel,
    onSurface = TextPrimary,
    surfaceVariant = Surface,
    onSurfaceVariant = TextSecondary,
    outline = BorderStroke
)

@Composable
fun HostelGainsTheme(
    darkTheme: Boolean = true, // Always enforce dark theme for fitness aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
