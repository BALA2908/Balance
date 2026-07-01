package com.balance.budget.data.repository

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.balance.budget.data.categorize.CategorizerStore
import com.balance.budget.data.local.BudgetDatabase
import com.balance.budget.data.preferences.SettingsRepository
import com.balance.budget.widget.SafeToSpendWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory reset — wipes the app back to a fresh install. Erases every Room table,
 * every preference, and the learned categorizer data, then re-seeds the default
 * categories & accounts so the app isn't left empty, and relaunches into
 * onboarding (clearing the preferences flips firstLaunchComplete back to false).
 *
 * This is intentionally destructive and irreversible; the UI must confirm first.
 */
@Singleton
class ResetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: BudgetDatabase,
    private val settings: SettingsRepository,
    private val categorizerStore: CategorizerStore,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
) {
    /** Erase everything, re-seed defaults, refresh the widget. */
    suspend fun resetAll() {
        // Room: clear all rows in all tables (keeps schema/version — no migration).
        database.clearAllTables()
        // Preferences + learned categorization.
        settings.clearAll()
        categorizerStore.clearAll()
        // Re-seed so the app opens with the default categories & wallets, not empty.
        categoryRepository.ensureSeeded()
        accountRepository.ensureSeeded()
        // The home-screen widget should reflect the wiped state.
        runCatching { SafeToSpendWidget().updateAll(context) }
    }

    /**
     * Relaunch the app on a fresh task so Compose + ViewModels rebuild against the
     * wiped database and cleared preferences (which now re-onboards).
     */
    fun relaunch() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (intent != null) context.startActivity(intent)
    }
}
