package com.kinetic.trainer.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/** M3 Expressive Motion — Spring-based animations for natural feel. */
object TrainerMotion {
    fun <T> snappy() = spring<T>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    fun <T> standard() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    fun <T> expressive() = spring<T>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
}
