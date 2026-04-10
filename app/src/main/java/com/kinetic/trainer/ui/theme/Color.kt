package com.kinetic.trainer.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

internal data class TrainerPalette(
    val lime: Color,
    val limeDark: Color,
    val background: Color,
    val surface1: Color,
    val surface2: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val error: Color,
    val warning: Color,
    val urgent: Color
)

internal val DarkTrainerPalette = TrainerPalette(
    lime = Color(0xFFD1FF26),
    limeDark = Color(0xFF8AB800),
    background = Color(0xFF09090B),
    surface1 = Color(0xFF18181B),
    surface2 = Color(0xFF27272A),
    textPrimary = Color(0xFFF4F4F5),
    textMuted = Color(0xFF71717A),
    error = Color(0xFFFF4444),
    warning = Color(0xFFFF9800),
    urgent = Color(0xFFFF4444)
)

private var activePalette by mutableStateOf(DarkTrainerPalette)

internal fun setActivePalette(useDark: Boolean) {
    activePalette = DarkTrainerPalette
}

val Lime: Color get() = activePalette.lime
val LimeDark: Color get() = activePalette.limeDark
val Background: Color get() = activePalette.background
val Surface1: Color get() = activePalette.surface1
val Surface2: Color get() = activePalette.surface2
val TextPrimary: Color get() = activePalette.textPrimary
val TextMuted: Color get() = activePalette.textMuted
val Error: Color get() = activePalette.error
val Warning: Color get() = activePalette.warning
val Urgent: Color get() = activePalette.urgent
