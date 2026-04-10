package com.kinetic.trainer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val TrainerDarkColorScheme = darkColorScheme(
    primary = DarkTrainerPalette.lime,
    onPrimary = DarkTrainerPalette.background,
    primaryContainer = DarkTrainerPalette.limeDark,
    onPrimaryContainer = DarkTrainerPalette.lime,
    secondary = DarkTrainerPalette.surface1,
    onSecondary = DarkTrainerPalette.textPrimary,
    secondaryContainer = DarkTrainerPalette.surface2,
    onSecondaryContainer = DarkTrainerPalette.textPrimary,
    background = DarkTrainerPalette.background,
    onBackground = DarkTrainerPalette.textPrimary,
    surface = DarkTrainerPalette.surface1,
    onSurface = DarkTrainerPalette.textPrimary,
    surfaceVariant = DarkTrainerPalette.surface2,
    onSurfaceVariant = DarkTrainerPalette.textMuted,
    error = DarkTrainerPalette.error,
    onError = DarkTrainerPalette.background,
    outline = DarkTrainerPalette.textMuted
)

@Composable
fun KineticTrainerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    SideEffect {
        setActivePalette(darkTheme)
        if (!view.isInEditMode) {
            val window = (view.context as Activity).window
            window.statusBarColor = TrainerDarkColorScheme.background.toArgb()
            window.navigationBarColor = TrainerDarkColorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = TrainerDarkColorScheme,
        typography = TrainerTypography,
        content = content
    )
}
