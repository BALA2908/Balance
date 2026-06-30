package com.balance.budget.domain.analytics

import com.balance.budget.domain.model.Category
import com.balance.budget.domain.model.ExpenseWithCategory

/**
 * The immutable input bundle the [AnalyticsEngine] consumes. Assembled by
 * `AnalyticsRepository` from the repository Flows; the engine itself never
 * touches Room, Flow, or Android — which is what makes it a pure, table-testable
 * function. All money is [Long] paise.
 */
data class AnalyticsInputs(
    /** "Now" in epoch millis — passed in so the engine stays pure/testable. */
    val nowMillis: Long,
    val monthExpenses: List<ExpenseWithCategory>,
    /** Previous calendar month's expenses, for month-over-month comparison. */
    val prevMonthExpenses: List<ExpenseWithCategory>,
    /** Active categories (for names/colors and budgeted-but-zero rows). */
    val categories: List<Category>,
    val overallBudgetMinor: Long?,            // null = no overall budget set
    val categoryBudgetsMinor: Map<Long, Long>, // categoryId -> budget paise
    /** Sum of all active recurring commitments (paise). */
    val activeRecurringTotalMinor: Long,
    /** This month's expenses already posted with source = RECURRING (paise). */
    val recurringPaidThisMonthMinor: Long,
    // --- Rollover (Wave 6). All default to the no-rollover, no-moves behavior so
    //     the engine is unchanged for existing installs / older tests. ----------
    /** When false, no carry is computed and safe-to-spend is unaffected. */
    val rolloverEnabled: Boolean = false,
    /** Previous month's overall budget — for the headline "rolled over" amount. */
    val prevOverallBudgetMinor: Long? = null,
    /** Previous month's per-category budgets — for per-category carry. */
    val prevCategoryBudgetsMinor: Map<Long, Long> = emptyMap(),
    /** "Move budget between categories" entries recorded for this month. */
    val monthAdjustments: List<BudgetAdjustment> = emptyList(),
    /** Envelope (zero-based) mode: safe-to-spend uses the sum of category envelopes. */
    val envelopeMode: Boolean = false,
    /** Optional monthly income (paise); null/0 = unset. Powers savings-rate health. */
    val monthlyIncomeMinor: Long? = null,
)

/**
 * A pure "move budget" record (the engine's view of a `budget_adjustments` row).
 * A null side means the unallocated/overall pool. Reallocations net to zero across
 * categories, so they reshape per-category envelopes without changing the overall
 * safe-to-spend pool.
 */
data class BudgetAdjustment(
    val fromCategoryId: Long?,
    val toCategoryId: Long?,
    val amountMinor: Long,
)
