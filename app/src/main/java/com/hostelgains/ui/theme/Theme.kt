package com.hostelgains.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private fun HgColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = ignite,
        onPrimary = onIgnite,
        primaryContainer = ignite.copy(alpha = 0.16f),
        onPrimaryContainer = ignite,
        secondary = info,
        onSecondary = surface1,
        tertiary = success,
        onTertiary = surface1,
        background = canvas,
        onBackground = textHigh,
        surface = surface1,
        onSurface = textHigh,
        surfaceVariant = surface2,
        onSurfaceVariant = textMid,
        surfaceContainerLowest = canvas,
        surfaceContainerLow = surface1,
        surfaceContainer = surface1,
        surfaceContainerHigh = surface2,
        surfaceContainerHighest = surface3,
        outline = outlineStrong,
        outlineVariant = outline,
        error = danger,
        onError = surface1
    )
}

/**
 * App theme. Follows the system light/dark setting by default; the palette is exposed
 * both as a Material 3 colour scheme and through [LocalHgColors] for custom tokens.
 */
@Composable
fun HostelGainsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkHgColors else LightHgColors
    CompositionLocalProvider(LocalHgColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
