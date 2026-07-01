package com.balance.budget.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.data.repository.AnalyticsRepository
import com.balance.budget.data.repository.BudgetAdjustmentRepository
import com.balance.budget.data.repository.BudgetRepository
import com.balance.budget.data.repository.CategoryRepository
import com.balance.budget.data.repository.ExpenseRepository
import com.balance.budget.data.preferences.SettingsRepository
import com.balance.budget.domain.analytics.BudgetTemplate
import com.balance.budget.domain.analytics.CategorySlice
import com.balance.budget.domain.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class BudgetsUiState(
    val overallBudgetMinor: Long? = null,
    val categories: List<Category> = emptyList(),
    val categoryBudgets: Map<Long, Long> = emptyMap(),
    val rolloverEnabled: Boolean = false,
    /** Per-category computed detail from the engine (carry-in, effective budget). */
    val slices: Map<Long, CategorySlice> = emptyMap(),
    /** Overall amount rolled over from last month into this month (0 when off). */
    val rolloverCarryMinor: Long = 0,
    /** Optional monthly income (paise); null = unset. Gates the template feature. */
    val monthlyIncomeMinor: Long? = null,
)

/**
 * Lets the user set/edit the overall monthly budget and optional per-category
 * limits. Writes are versioned by month (via [BudgetRepository]) so editing this
 * month never rewrites a prior month's limit. Rollover carry / effective budgets
 * come from the deterministic engine (via [AnalyticsRepository]) — never recomputed
 * here — and "move budget" records one [BudgetAdjustmentRepository] entry.
 */
@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val budgetAdjustmentRepository: BudgetAdjustmentRepository,
    categoryRepository: CategoryRepository,
    analyticsRepository: AnalyticsRepository,
    private val expenseRepository: ExpenseRepository,
    private val settings: SettingsRepository,
    private val clock: () -> Long,
) : ViewModel() {

    private val month: YearMonth = DateTimeUtil.yearMonth(clock())
    private val monthKey: Int = DateTimeUtil.yearMonthKey(month)

    // combine() tops out at five typed flows — fold the two settings into a Pair.
    private val settingsFlow = combine(
        settings.rolloverEnabled,
        settings.monthlyIncomeMinor,
    ) { rollover, income -> rollover to income }

    val state: StateFlow<BudgetsUiState> = combine(
        budgetRepository.observeOverallBudget(month),
        budgetRepository.observeCategoryBudgets(month),
        categoryRepository.observeActive(),
        analyticsRepository.snapshot,
        settingsFlow,
    ) { overall, catBudgets, cats, snapshot, s ->
        BudgetsUiState(
            overallBudgetMinor = overall?.amountMinor,
            categories = cats,
            categoryBudgets = catBudgets.mapNotNull { b -> b.categoryId?.let { it to b.amountMinor } }.toMap(),
            rolloverEnabled = s.first,
            slices = snapshot.byCategory.associateBy { it.categoryId },
            rolloverCarryMinor = snapshot.rolloverCarryMinor,
            monthlyIncomeMinor = s.second,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState())

    fun setOverallBudget(amountMinor: Long) = viewModelScope.launch {
        budgetRepository.setOverallBudget(amountMinor, month)
    }

    fun setCategoryBudget(categoryId: Long, amountMinor: Long) = viewModelScope.launch {
        budgetRepository.setCategoryBudget(categoryId, amountMinor, month)
    }

    /** Reallocate this month's budget from one category to another. */
    fun moveBudget(fromCategoryId: Long, toCategoryId: Long, amountMinor: Long) = viewModelScope.launch {
        if (fromCategoryId == toCategoryId || amountMinor <= 0L) return@launch
        budgetAdjustmentRepository.move(monthKey, fromCategoryId, toCategoryId, amountMinor)
    }

    /** Clear every budget — overall + all per-category limits. Expenses kept. */
    fun clearAllBudgets() = viewModelScope.launch {
        budgetRepository.clearAllBudgets()
    }

    /** Remove the limit for a single category. Expenses kept. */
    fun clearCategoryBudget(categoryId: Long) = viewModelScope.launch {
        budgetRepository.clearCategoryBudget(categoryId)
    }

    /**
     * Set this month's budgets from income using a 50/30/20-style split. Overall =
     * income × (1 − savings%); per-category limits follow last month's actual spend
     * shares (skipped when there's no history). Needs income set (no-op otherwise).
     */
    fun applyTemplate(savingsRatePercent: Int) = viewModelScope.launch {
        val income = settings.monthlyIncomeMinor.first() ?: return@launch
        val prevMonthByCategory = expenseRepository.observeForMonth(month.minusMonths(1)).first()
            .groupBy { it.expense.categoryId }
            .mapValues { (_, rows) -> rows.sumOf { it.expense.amountMinor } }
        val result = BudgetTemplate.fromIncome(income, savingsRatePercent, prevMonthByCategory)
        if (result.overallMinor <= 0L) return@launch
        budgetRepository.setOverallBudget(result.overallMinor, month)
        result.perCategoryMinor.forEach { (categoryId, amount) ->
            budgetRepository.setCategoryBudget(categoryId, amount, month)
        }
    }
}
