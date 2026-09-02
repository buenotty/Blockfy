package com.robingebert.blokky.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BlockfyDarkColorScheme = darkColorScheme(
    primary = BlockfyPrimary,
    onPrimary = Color.White,
    primaryContainer = BlockfySurfaceHigh,
    onPrimaryContainer = BlockfyTertiary,
    secondary = BlockfySecondary,
    onSecondary = Color.White,
    secondaryContainer = BlockfySurfaceVariant,
    onSecondaryContainer = BlockfyTextPrimary,
    tertiary = BlockfyTertiary,
    background = BlockfyBackground,
    onBackground = BlockfyTextPrimary,
    surface = BlockfySurface,
    onSurface = BlockfyTextPrimary,
    surfaceVariant = BlockfySurfaceVariant,
    onSurfaceVariant = BlockfyTextSecondary,
    surfaceContainer = BlockfySurfaceVariant,
    surfaceContainerHigh = BlockfySurfaceHigh,
    outline = BlockfyOutline,
    outlineVariant = BlockfyOutlineVariant,
    error = BlockfyError,
    onError = Color.White,
    errorContainer = Color(0x33F43F5E)
)

private val BlockfyLightColorScheme = lightColorScheme(
    primary = BlockfyPrimaryVariant,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = BlockfySecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF0F172A),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = BlockfyError,
    onError = Color.White
)

@Composable
fun BlokkyTheme(
    darkTheme: Boolean = true, // Default to sleek dark mode matching the logo
    dynamicColor: Boolean = false, // Keep signature Blockfy brand styling
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BlockfyDarkColorScheme else BlockfyLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}