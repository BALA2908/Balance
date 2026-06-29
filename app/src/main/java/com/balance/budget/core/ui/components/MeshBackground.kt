package com.balance.budget.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * A subtle, ambient mesh-gradient wash — two warm radial blooms that drift
 * slowly over the background. It's the quiet "alive, not corporate" touch behind
 * the hero. Amplitude is deliberately low so it reads as calm, never busy.
 *
 * When [reduceMotion] is true (or for accessibility), the blooms are placed
 * statically with no animation.
 */
@Composable
fun Modifier.meshBackground(
    reduceMotion: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
    secondary: Color = MaterialTheme.colorScheme.tertiary,
): Modifier {
    val phase: Float
    if (reduceMotion) {
        phase = 0f
    } else {
        val transition = rememberInfiniteTransition(label = "mesh")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 22_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "mesh-phase",
        )
        phase = animated
    }

    return this.drawBehind {
        val w = size.width
        val h = size.height

        // Bloom 1 — warm accent, upper area, drifts on a small ellipse.
        val c1 = Offset(
            x = w * (0.30f + 0.10f * cos(phase)),
            y = h * (0.18f + 0.05f * sin(phase)),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                center = c1,
                radius = w * 0.9f,
            ),
            size = size,
        )

        // Bloom 2 — secondary tint, lower-right, drifts in counter-phase.
        val c2 = Offset(
            x = w * (0.80f + 0.08f * cos(phase + Math.PI.toFloat())),
            y = h * (0.72f + 0.06f * sin(phase + Math.PI.toFloat())),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.10f), Color.Transparent),
                center = c2,
                radius = w * 0.8f,
            ),
            size = size,
        )
    }
}

/** Convenience wrapper that fills available space with the mesh wash. */
@Composable
fun MeshBackground(reduceMotion: Boolean = false) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .meshBackground(reduceMotion = reduceMotion)
    )
}
