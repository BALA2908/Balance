package com.balance.budget.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkScheme = darkColorScheme(
    primary = CozyColors.Amber,
    onPrimary = CozyColors.Espresso,
    primaryContainer = CozyColors.AmberDeep,
    onPrimaryContainer = CozyColors.Cream,
    secondary = CozyColors.AmberSoft,
    onSecondary = CozyColors.Espresso,
    background = CozyColors.Espresso,
    onBackground = CozyColors.Cream,
    surface = CozyColors.Mocha,
    onSurface = CozyColors.Cream,
    surfaceVariant = CozyColors.Walnut,
    onSurfaceVariant = CozyColors.Latte,
    outline = CozyColors.Sand,
    outlineVariant = CozyColors.Sand,
    error = CozyColors.Clay,
    onError = CozyColors.Espresso,
    tertiary = CozyColors.Sage,
)

private val LightScheme = lightColorScheme(
    primary = CozyColors.AmberDeep,
    onPrimary = CozyColors.Paper,
    background = CozyColors.Paper,
    onBackground = CozyColors.InkOnPaper,
    surface = CozyColors.PaperRaised,
    onSurface = CozyColors.InkOnPaper,
    surfaceVariant = Color(0xFFF0E7DA),
    onSurfaceVariant = CozyColors.Taupe,
    outline = Color(0xFFD9CDBC),
    error = CozyColors.Clay,
    tertiary = CozyColors.Sage,
)

/**
 * App theme. Dark-first: the cozy aesthetic is designed for dark. We intentionally
 * do NOT use Material You dynamic color — the warm amber identity is the point.
 */
@Composable
fun BalanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = BalanceTypography,
        shapes = BalanceShapes,
        content = content,
    )
}
