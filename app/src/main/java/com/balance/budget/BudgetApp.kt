package com.balance.budget

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.balance.budget.data.di.ApplicationScope
import com.balance.budget.data.repository.AccountRepository
import com.balance.budget.data.repository.CategoryRepository
import com.balance.budget.data.repository.ExpenseRepository
import com.balance.budget.data.repository.RecurringMaterializerRunner
import androidx.glance.appwidget.updateAll
import com.balance.budget.widget.SafeToSpendWidget
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point. [HiltAndroidApp] triggers Hilt's code generation and
 * creates the application-level dependency container.
 *
 * On first launch we seed the default categories so the Quick Add screen always
 * has something to pick from. Seeding is idempotent (no-op if categories exist).
 */
@HiltAndroidApp
class BudgetApp : Application(), Configuration.Provider {

    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var accountRepository: AccountRepository
    @Inject lateinit var expenseRepository: ExpenseRepository
    @Inject lateinit var recurringMaterializer: RecurringMaterializerRunner
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            categoryRepository.ensureSeeded()
            accountRepository.ensureSeeded()
            // Generate any due recurring expenses (idempotent; catches up missed periods).
            runCatching { recurringMaterializer.run() }
        }
        // Keep the home-screen widget fresh: refresh whenever the expense count
        // changes (add/delete). Drop the initial emission so we don't update on boot.
        appScope.launch {
            expenseRepository.observeCount().drop(1).collect {
                runCatching { SafeToSpendWidget().updateAll(this@BudgetApp) }
            }
        }
    }
}
