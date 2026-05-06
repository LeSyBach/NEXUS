package com.example.nexus.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ══════════════════════════════════════════════════════════════
// NEXUS Theme - Material 3 with Custom Color System
// ══════════════════════════════════════════════════════════════

private val NexusDarkColorScheme = darkColorScheme(
    primary = NexusPrimary,
    onPrimary = Color.Black,
    primaryContainer = NexusPrimaryDark,
    onPrimaryContainer = NexusPrimaryLight,
    secondary = NexusSecondary,
    onSecondary = Color.Black,
    secondaryContainer = NexusSecondaryDark,
    onSecondaryContainer = NexusSecondary,
    tertiary = NexusAccent,
    onTertiary = Color.White,
    tertiaryContainer = NexusAccent,
    onTertiaryContainer = NexusAccentLight,
    error = NexusError,
    onError = Color.White,
    errorContainer = NexusError.copy(alpha = 0.3f),
    onErrorContainer = NexusErrorLight,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    inversePrimary = NexusPrimaryDark,
    surfaceTint = NexusPrimary,
)

private val NexusLightColorScheme = lightColorScheme(
    primary = NexusPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = NexusPrimaryLight,
    onPrimaryContainer = NexusPrimaryDark,
    secondary = NexusSecondaryDark,
    onSecondary = Color.White,
    secondaryContainer = NexusSecondary.copy(alpha = 0.2f),
    onSecondaryContainer = NexusSecondaryDark,
    tertiary = NexusAccent,
    onTertiary = Color.White,
    tertiaryContainer = NexusAccentLight.copy(alpha = 0.3f),
    onTertiaryContainer = NexusAccent,
    error = NexusError,
    onError = Color.White,
    errorContainer = NexusError.copy(alpha = 0.1f),
    onErrorContainer = NexusError,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkOnSurface,
    inversePrimary = NexusPrimary,
    surfaceTint = NexusPrimaryDark,
)

@Composable
fun NEXUSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NexusDarkColorScheme else NexusLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NexusTypography,
        shapes = NexusShapes,
        content = content
    )
}