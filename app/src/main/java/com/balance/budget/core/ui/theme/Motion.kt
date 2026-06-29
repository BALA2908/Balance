package com.balance.budget.core.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * One place for all motion. Motion is a core part of the feel here, not
 * decoration — springy where things should feel physical (sheets, buttons,
 * the save morph) and smooth-tween where things should feel composed (counts,
 * chart draw-in). Keep transitions calm: nothing snappy enough to feel jarring.
 */
object Motion {

    /** The add sheet slides up / settles with a gentle, slightly bouncy spring. */
    fun <T> sheetSpring(): AnimationSpec<T> = spring(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Button press / chip selection — quick, low-bounce. */
    fun <T> tapSpring(): AnimationSpec<T> = spring(
        dampingRatio = 0.6f,
        stiffness = Spring.StiffnessMedium,
    )

    /** The save checkmark morph — a touch of overshoot to feel satisfying. */
    fun <T> morphSpring(): AnimationSpec<T> = spring(
        dampingRatio = 0.55f,
        stiffness = Spring.StiffnessMedium,
    )

    /** Count-up number animation on dashboard totals. */
    fun <T> countUp(): AnimationSpec<T> = tween(durationMillis = 900, easing = FastOutSlowInEasing)

    /** Charts draw in over this duration (used from Phase 3). */
    fun <T> chartDraw(): AnimationSpec<T> = tween(durationMillis = 750, easing = FastOutSlowInEasing)

    const val COUNT_UP_MS = 900
}
