package com.balance.budget.domain.analytics

import com.balance.budget.domain.model.BudgetState
import java.time.YearMonth

/**
 * Result models for the deterministic [AnalyticsEngine]. Every value here is
 * computed in pure Kotlin from real data — the AI layer never produces or alters
 * any of these numbers. All money is [Long] paise.
 */

enum class TrendDirection { UP, DOWN, FLAT }

/** A comparison of a current period against a previous one. */
data class Trend(
    val currentMinor: Long,
    val previousMinor: Long,
    val deltaMinor: Long,
    /** Percentage change vs previous; null when previous == 0 (undefined). */
    val percentChange: Double?,
    val direction: TrendDirection,
)

/** One category's slice of the month: spend, share, and budget status. */
data class CategorySlice(
    val categoryId: Long,
    val name: String,
    val colorHex: String,
    val iconKey: String,
    val spentMinor: Long,
    val percentOfTotal: Double,      // 0.0..100.0
    val budgetMinor: Long?,          // the BASE per-category budget; null = none
    val remainingMinor: Long?,       // effectiveBudget - spent, when budgeted
    val isOverBudget: Boolean,
    val state: BudgetState,
    // --- Rollover (Wave 6); all zero/equal-to-base when rollover is off --------
    /** Positive unspent budget carried in from last month (0 when off). */
    val carryInMinor: Long = 0,
    /** Net of "move budget" reallocations this month (+ in, − out). */
    val adjustmentMinor: Long = 0,
    /** budget + carryIn + adjustment; equals [budgetMinor] when off & no moves. */
    val effectiveBudgetMinor: Long? = null,
)

/** A merchant the user spent at this month (from auto-import / notes). */
data class MerchantSlice(
    val merchant: String,
    val spentMinor: Long,
    val count: Int,
)

/** Linear month-end projection from current pace. */
data class Projection(
    val projectedMonthEndMinor: Long,
    val budgetMinor: Long?,
    val projectedOverBudgetMinor: Long?, // max(0, projected - budget) when budgeted
    val onTrack: Boolean?,               // projected <= budget, when budgeted
    val daysElapsed: Int,
    val daysRemaining: Int,
)

enum class SafeToSpendBasis {
    FULL,        // a budget exists and there's money left
    NO_BUDGET,   // no overall budget set — prompt the user to set one
    EXHAUSTED,   // budget set but nothing left for the rest of the month
}

/** The calm, guilt-free amount available per remaining day. */
data class SafeToSpend(
    val perDayMinor: Long,
    val remainingPoolMinor: Long,
    val daysRemaining: Int,
    val basis: SafeToSpendBasis,
)

/** An unusually large expense for its category (> mean + 2σ). */
data class Anomaly(
    val expenseId: Long,
    val categoryId: Long,
    val categoryName: String,
    val amountMinor: Long,
    val categoryMeanMinor: Long,
    val categoryStdDevMinor: Long,
    val sigmaOver: Double,
)

/** Encouraging streaks — never punitive. */
data class Streaks(
    val currentUnderBudgetDays: Int,
    val longestUnderBudgetDays: Int,
    /** % of completed days this month that stayed under the daily budget (0 when no budget). */
    val adherencePercent: Double = 0.0,
)

/**
 * The single, immutable snapshot of everything the dashboard / reports / AI
 * layer read. Produced by [AnalyticsEngine.compute].
 */
data class AnalyticsSnapshot(
    val month: YearMonth,
    val monthToDateMinor: Long,
    val overallBudgetMinor: Long?,
    val overallRemainingMinor: Long?,
    val overallState: BudgetState,
    val byCategory: List<CategorySlice>,   // desc by spent; includes budgeted-but-zero
    val topCategories: List<CategorySlice>, // first N with spent > 0
    val topMerchants: List<MerchantSlice>,
    val dailyAverageMinor: Long,
    val weeklyAverageMinor: Long,
    val weekOverWeek: Trend,
    val monthOverMonth: Trend,
    val projection: Projection,
    val safeToSpend: SafeToSpend,
    val streaks: Streaks,
    val anomalies: List<Anomaly>,
    val isEmpty: Boolean,
    /** Total amount rolled over from last month into this month's safe-to-spend
     *  (0 when rollover is off or last month had no leftover). */
    val rolloverCarryMinor: Long = 0,
    /** Sum of active recurring commitments (paise) — surfaced for the health view. */
    val activeRecurringTotalMinor: Long = 0,
    /** Financial-health evaluation (discipline, savings, investment) — null when
     *  there's nothing meaningful to show yet. */
    val financialHealth: FinancialHealth? = null,
) {
    companion object {
        /** A zeroed snapshot for a month with no data (and the initial UI state). */
        fun empty(month: YearMonth): AnalyticsSnapshot {
            val flatTrend = Trend(0, 0, 0, null, TrendDirection.FLAT)
            return AnalyticsSnapshot(
                month = month,
                monthToDateMinor = 0,
                overallBudgetMinor = null,
                overallRemainingMinor = null,
                overallState = BudgetState.UNDER,
                byCategory = emptyList(),
                topCategories = emptyList(),
                topMerchants = emptyList(),
                dailyAverageMinor = 0,
                weeklyAverageMinor = 0,
                weekOverWeek = flatTrend,
                monthOverMonth = flatTrend,
                projection = Projection(0, null, null, null, 0, 0),
                safeToSpend = SafeToSpend(0, 0, 0, SafeToSpendBasis.NO_BUDGET),
                streaks = Streaks(0, 0),
                anomalies = emptyList(),
                isEmpty = true,
                rolloverCarryMinor = 0,
            )
        }
    }
}
