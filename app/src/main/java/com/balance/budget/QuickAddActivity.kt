package com.balance.budget

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.balance.budget.core.ui.theme.BalanceTheme
import com.balance.budget.core.util.Money
import com.balance.budget.data.preferences.SettingsRepository
import com.balance.budget.domain.model.ExpenseSource
import com.balance.budget.domain.model.ThemeMode
import com.balance.budget.feature.quickadd.QuickAddBottomSheet
import com.balance.budget.feature.quickadd.QuickAddViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The dedicated external entry point into Quick Add.
 *
 * Reached two ways from OUTSIDE the app, both landing on the exact same sheet
 * and the exact same save path as the in-app FAB:
 *   1. By component  — Samsung Good Lock → RegiStar launches this Activity in
 *      response to the double-tap-back gesture (configured on the phone, not
 *      here — the app never tries to capture the gesture itself).
 *   2. By deep link  — `balance://quickadd` (also usable from a home-screen
 *      shortcut, adb, or other apps).
 *
 * Optional deep-link query params let an external source pre-fill the sheet:
 *   balance://quickadd?amount=149.50&note=Coffee&merchant=Third%20Wave
 */
@AndroidEntryPoint
class QuickAddActivity : ComponentActivity() {

    private val viewModel: QuickAddViewModel by viewModels()

    @Inject lateinit var settings: SettingsRepository

    /** Toggles blur-behind on/off as the system enables/disables cross-window blur. */
    private val blurListener = java.util.function.Consumer<Boolean> { enabled ->
        val radiusPx = (BLUR_RADIUS_DP * resources.displayMetrics.density).toInt()
        window.attributes = window.attributes.apply {
            blurBehindRadius = if (enabled) radiusPx else 0
        }
        // With live blur a whisper of dim is enough; without it, lean on a heavier
        // scrim so the glass panels still read over a busy home screen.
        window.setDimAmount(if (enabled) 0.18f else 0.5f)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        applyWindowBlur()
        applyDeepLinkPrefill()

        setContent {
            // Match the app's theme choice (dark-first) so the popup is on-brand,
            // not following the system theme independently.
            val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.DARK)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            BalanceTheme(darkTheme = darkTheme) {
                // Sheet over a dimmed, transparent window — feels like it floats
                // above whatever was on screen. Dismiss (swipe/scrim/save) finishes.
                QuickAddBottomSheet(
                    onDismiss = { finish() },
                    viewModel = viewModel,
                )
            }
        }
    }

    /**
     * Frosts whatever is behind the popup (home screen, another app) so the glass
     * panels float over a soft blur — the "crystal" backdrop, present even when the
     * popup is fired repeatedly by double-tap-back. Cross-window blur is gated by the
     * OS (GPU support, battery saver, the "allow window-level blurs" dev/system flag),
     * so we register a listener and fall back to a plain dim scrim when it's off —
     * never a crash, never a black window.
     */
    private fun applyWindowBlur() {
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        // Fires immediately with the current state, then again whenever it changes.
        windowManager.addCrossWindowBlurEnabledListener(blurListener)
    }

    override fun onDestroy() {
        windowManager.removeCrossWindowBlurEnabledListener(blurListener)
        super.onDestroy()
    }

    private fun applyDeepLinkPrefill() {
        val data = intent?.data ?: return
        val amountMinor = data.getQueryParameter("amount")?.let { Money.parseToMinor(it) }
        val note = data.getQueryParameter("note")
        val merchant = data.getQueryParameter("merchant")
        if (amountMinor != null || note != null || merchant != null) {
            viewModel.prefill(
                amountMinor = amountMinor,
                note = note,
                merchant = merchant,
                source = ExpenseSource.MANUAL,
            )
        }
    }

    private companion object {
        const val BLUR_RADIUS_DP = 56f
    }
}
