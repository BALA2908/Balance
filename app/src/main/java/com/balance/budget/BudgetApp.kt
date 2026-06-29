package com.balance.budget

import android.app.Application
import com.balance.budget.data.di.ApplicationScope
import com.balance.budget.data.repository.CategoryRepository
import com.balance.budget.data.repository.RecurringMaterializerRunner
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
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
class BudgetApp : Application() {

    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var recurringMaterializer: RecurringMaterializerRunner
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            categoryRepository.ensureSeeded()
            // Generate any due recurring expenses (idempotent; catches up missed periods).
            runCatching { recurringMaterializer.run() }
        }
    }
}
