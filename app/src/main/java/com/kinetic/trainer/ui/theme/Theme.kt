package com.kinetic.trainer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val TrainerShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

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
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicDarkColorScheme(LocalContext.current)
        else -> TrainerDarkColorScheme
    }
    val view = LocalView.current

    SideEffect {
        setActivePalette(darkTheme)
        if (!view.isInEditMode) {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TrainerTypography,
        shapes = TrainerShapes,
        content = content
    )
}
