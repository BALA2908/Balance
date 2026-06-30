package com.balance.budget.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.balance.budget.R
import com.balance.budget.feature.accounts.AccountManagerScreen
import com.balance.budget.feature.budgets.BudgetsScreen
import com.balance.budget.feature.categories.CategoryManagerScreen
import com.balance.budget.feature.tags.TagManagerScreen
import com.balance.budget.feature.rules.RulesScreen
import com.balance.budget.feature.bills.BillsScreen
import com.balance.budget.feature.goals.SavingsGoalsScreen
import com.balance.budget.feature.networth.NetWorthScreen
import com.balance.budget.feature.assistant.AskScreen
import com.balance.budget.feature.dashboard.DashboardScreen
import com.balance.budget.feature.history.HistoryScreen
import com.balance.budget.feature.imports.ImportReviewScreen
import com.balance.budget.feature.moneystory.MoneyStoryScreen
import com.balance.budget.feature.quickadd.QuickAddBottomSheet
import com.balance.budget.feature.recurring.RecurringScreen
import com.balance.budget.feature.reports.ReportsScreen
import com.balance.budget.feature.settings.SettingsScreen

/**
 * The single app shell: one Scaffold owning the bottom nav and the ＋ FAB (shown
 * only on the dashboard), with the NavHost as content. Quick Add lives here as a
 * sheet — never a tab — so the FAB and the external deep link both reach the one
 * save path. Pushed routes (Budgets) hide the bottom bar and draw their own back.
 */
@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTab = currentRoute in BottomTab.routes
    var showQuickAdd by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isTab) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    BottomTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Routes.DASHBOARD) {
                ExtendedFloatingActionButton(
                    onClick = { showQuickAdd = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.shortcut_add_long)) },
                )
            }
        },
    ) { padding ->
        NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    contentPadding = padding,
                    onNavigateToBudgets = { navController.navigate(Routes.BUDGETS) },
                    onNavigateToAsk = { navController.navigate(Routes.ASK) },
                    onNavigateToMoneyStory = { navController.navigate(Routes.MONEY_STORY) },
                )
            }
            composable(Routes.REPORTS) {
                ReportsScreen(contentPadding = padding)
            }
            composable(Routes.HISTORY) {
                HistoryScreen(contentPadding = padding)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    contentPadding = padding,
                    onNavigateToBudgets = { navController.navigate(Routes.BUDGETS) },
                    onNavigateToRecurring = { navController.navigate(Routes.RECURRING) },
                    onNavigateToImportReview = { navController.navigate(Routes.IMPORT_REVIEW) },
                    onNavigateToCategories = { navController.navigate(Routes.CATEGORIES) },
                    onNavigateToAccounts = { navController.navigate(Routes.ACCOUNTS) },
                    onNavigateToTags = { navController.navigate(Routes.TAGS) },
                    onNavigateToRules = { navController.navigate(Routes.RULES) },
                    onNavigateToBills = { navController.navigate(Routes.BILLS) },
                    onNavigateToGoals = { navController.navigate(Routes.GOALS) },
                    onNavigateToNetWorth = { navController.navigate(Routes.NET_WORTH) },
                )
            }
            composable(Routes.BUDGETS) {
                BudgetsScreen(
                    contentPadding = padding,
                    onBack = { navController.popBackStack() },
                    onManageCategories = { navController.navigate(Routes.CATEGORIES) },
                )
            }
            composable(Routes.CATEGORIES) {
                CategoryManagerScreen(
                    contentPadding = padding,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ACCOUNTS) {
                AccountManagerScreen(
                    contentPadding = padding,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.TAGS) {
                TagManagerScreen(
                    contentPadding = padding,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.RULES) {
                RulesScreen(
                    contentPadding = padding,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.BILLS) {
                BillsScreen(
                    contentPadding = padding,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.GOALS) {
                SavingsGoalsScreen(
                    contentPadding = padding,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.NET_WORTH) {
                NetWorthScreen(
                    contentPadding = padding,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.RECURRING) {
                RecurringScreen(
                    contentPadding = padding,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.IMPORT_REVIEW) {
                ImportReviewScreen(
                    contentPadding = padding,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ASK) {
                AskScreen(contentPadding = padding, onBack = { navController.popBackStack() })
            }
            composable(Routes.MONEY_STORY) {
                MoneyStoryScreen(onClose = { navController.popBackStack() })
            }
        }
    }

    if (showQuickAdd) {
        QuickAddBottomSheet(onDismiss = { showQuickAdd = false })
    }
}
