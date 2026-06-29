package com.balance.budget.data.repository

import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.data.di.ApplicationScope
import com.balance.budget.data.preferences.SettingsRepository
import com.balance.budget.domain.analytics.AnalyticsEngine
import com.balance.budget.domain.analytics.AnalyticsInputs
import com.balance.budget.domain.analytics.AnalyticsSnapshot
import com.balance.budget.domain.analytics.BudgetAdjustment
import com.balance.budget.domain.model.Budget
import com.balance.budget.domain.model.Category
import com.balance.budget.domain.model.ExpenseSource
import com.balance.budget.domain.model.ExpenseWithCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for computed insights. It fans the existing repository
 * Flows into the pure [AnalyticsEngine] and exposes one hot [snapshot] that the
 * dashboard, reports, and the AI layer all read — so the math is computed once.
 *
 * The engine takes full month rows (not a SQL aggregate): a single month is small,
 * and the engine needs the raw rows anyway for merchants, anomalies, and per-day
 * streaks. recurring-paid is derived in Kotlin from RECURRING-source rows.
 *
 * Note: the target month is resolved once at construction from the clock. Across
 * a month rollover within a single long-running process the snapshot would lag
 * until the process restarts — acceptable for v1 (documented).
 */
@Singleton
class AnalyticsRepository @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val recurringRepository: RecurringRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetAdjustmentRepository: BudgetAdjustmentRepository,
    private val settings: SettingsRepository,
    private val clock: () -> Long,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val month: YearMonth = DateTimeUtil.yearMonth(clock())
    private val prevMonth: YearMonth = month.minusMonths(1)
    private val monthKey: Int = DateTimeUtil.yearMonthKey(month)

    private data class Core(
        val monthExpenses: List<ExpenseWithCategory>,
        val prevMonthExpenses: List<ExpenseWithCategory>,
        val categories: List<Category>,
        val overallBudget: Budget?,
        val categoryBudgets: List<Budget>,
    )

    private data class Rollover(
        val prevCategoryBudgets: List<Budget>,
        val prevOverallBudget: Budget?,
        val recurringTotal: Long,
        val enabled: Boolean,
        val adjustments: List<BudgetAdjustment>,
    )

    val snapshot: StateFlow<AnalyticsSnapshot> = combine(
        // Group A — this month's raw material (5 flows = combine's typed limit).
        combine(
            expenseRepository.observeForMonth(month),
            expenseRepository.observeForMonth(prevMonth),
            categoryRepository.observeActive(),
            budgetRepository.observeOverallBudget(month),
            budgetRepository.observeCategoryBudgets(month),
        ) { monthEx, prevEx, cats, overall, catBudgets ->
            Core(monthEx, prevEx, cats, overall, catBudgets)
        },
        // Group B — rollover ingredients (prev-month budgets, the toggle, moves).
        combine(
            budgetRepository.observeCategoryBudgets(prevMonth),
            budgetRepository.observeOverallBudget(prevMonth),
            recurringRepository.observeActiveTotal(),
            settings.rolloverEnabled,
            budgetAdjustmentRepository.observeForMonth(monthKey),
        ) { prevCatBudgets, prevOverall, recurringTotal, enabled, adjustments ->
            Rollover(prevCatBudgets, prevOverall, recurringTotal, enabled, adjustments)
        },
    ) { core, roll ->
        val recurringPaid = core.monthExpenses
            .filter { it.expense.source == ExpenseSource.RECURRING }
            .sumOf { it.expense.amountMinor }
        AnalyticsEngine.compute(
            AnalyticsInputs(
                nowMillis = clock(),
                monthExpenses = core.monthExpenses,
                prevMonthExpenses = core.prevMonthExpenses,
                categories = core.categories,
                overallBudgetMinor = core.overallBudget?.amountMinor,
                categoryBudgetsMinor = core.categoryBudgets.toAmountMap(),
                activeRecurringTotalMinor = roll.recurringTotal,
                recurringPaidThisMonthMinor = recurringPaid,
                rolloverEnabled = roll.enabled,
                prevOverallBudgetMinor = roll.prevOverallBudget?.amountMinor,
                prevCategoryBudgetsMinor = roll.prevCategoryBudgets.toAmountMap(),
                monthAdjustments = roll.adjustments,
            )
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), AnalyticsSnapshot.empty(month))

    private fun List<Budget>.toAmountMap(): Map<Long, Long> =
        mapNotNull { b -> b.categoryId?.let { it to b.amountMinor } }.toMap()

    /**
     * A one-shot snapshot computed from the current data — for callers without a
     * long-lived subscription (e.g. the home-screen widget), where [snapshot]'s
     * value may be the cold initial.
     */
    suspend fun snapshotOnce(): AnalyticsSnapshot {
        val m = DateTimeUtil.yearMonth(clock())
        val prev = m.minusMonths(1)
        val monthEx = expenseRepository.observeForMonth(m).first()
        val recurringPaid = monthEx
            .filter { it.expense.source == ExpenseSource.RECURRING }
            .sumOf { it.expense.amountMinor }
        return AnalyticsEngine.compute(
            AnalyticsInputs(
                nowMillis = clock(),
                monthExpenses = monthEx,
                prevMonthExpenses = expenseRepository.observeForMonth(prev).first(),
                categories = categoryRepository.observeActive().first(),
                overallBudgetMinor = budgetRepository.observeOverallBudget(m).first()?.amountMinor,
                categoryBudgetsMinor = budgetRepository.observeCategoryBudgets(m).first().toAmountMap(),
                activeRecurringTotalMinor = recurringRepository.observeActiveTotal().first(),
                recurringPaidThisMonthMinor = recurringPaid,
                rolloverEnabled = settings.rolloverEnabled.first(),
                prevOverallBudgetMinor = budgetRepository.observeOverallBudget(prev).first()?.amountMinor,
                prevCategoryBudgetsMinor = budgetRepository.observeCategoryBudgets(prev).first().toAmountMap(),
                monthAdjustments = budgetAdjustmentRepository.getForMonth(DateTimeUtil.yearMonthKey(m)),
            )
        )
    }
}
