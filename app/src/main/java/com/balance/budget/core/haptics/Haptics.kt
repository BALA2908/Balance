package com.balance.budget.core.haptics

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Thin wrapper over the platform haptics. The Samsung S24 series has excellent
 * actuators, so we lean on distinct constants for distinct moments instead of a
 * single buzz everywhere. Each call is best-effort; haptics never block UI.
 *
 * Usage:
 *   val haptics = rememberHaptics()
 *   haptics.tick()      // light, for digit taps / chip selection
 *   haptics.confirm()   // satisfying, for a successful save
 *   haptics.reject()    // for an invalid action / over-budget warning
 */
class Haptics(private val view: View) {

    /** Light tactile tick — number pad presses, chip selection. */
    fun tick() = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

    /** A small click for toggles and selections. */
    fun select() = view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

    /** The "it worked" confirmation — fired on a successful expense save. */
    fun confirm() = view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

    /** A rejecting buzz — invalid input, or hitting a budget limit. */
    fun reject() = view.performHapticFeedback(HapticFeedbackConstants.REJECT)

    /** Long-press style heavier tap. */
    fun heavy() = view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
