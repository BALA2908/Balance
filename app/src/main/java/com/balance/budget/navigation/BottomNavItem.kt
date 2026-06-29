package com.balance.budget.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/** The four bottom-nav tabs. Order = display order. */
enum class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD(Routes.DASHBOARD, "Home", Icons.Rounded.Home),
    REPORTS(Routes.REPORTS, "Reports", Icons.Rounded.BarChart),
    HISTORY(Routes.HISTORY, "History", Icons.Rounded.ReceiptLong),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Rounded.Tune);

    companion object {
        val routes: Set<String> = entries.map { it.route }.toSet()
    }
}
