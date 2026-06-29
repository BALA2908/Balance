package com.balance.budget.domain.ai

import com.balance.budget.domain.analytics.AnalyticsSnapshot
import com.balance.budget.domain.analytics.CategorySlice
import com.balance.budget.domain.analytics.MerchantSlice
import com.balance.budget.domain.analytics.Projection
import com.balance.budget.domain.analytics.SafeToSpend
import com.balance.budget.domain.analytics.SafeToSpendBasis
import com.balance.budget.domain.analytics.Streaks
import com.balance.budget.domain.model.BudgetState
import java.time.YearMonth

/** Builds [AnalyticsSnapshot]s for AI-layer tests without running the engine. */
object AiTestData {

    const val SENTINEL_MERCHANT = "SECRET_MERCHANT_XYZ"

    fun slice(id: Long, name: String, spentMinor: Long, percent: Double) = CategorySlice(
        categoryId = id, name = name, colorHex = "#E0795B", iconKey = "food",
        spentMinor = spentMinor, percentOfTotal = percent,
        budgetMinor = null, remainingMinor = null, isOverBudget = false, state = BudgetState.UNDER,
    )

    fun snapshot(
        monthToDate: Long = 10_000_00,
        budget: Long? = 30_000_00,
        topCategories: List<CategorySlice> = listOf(
            slice(1, "Food", 6_000_00, 60.0),
            slice(2, "Travel", 4_000_00, 40.0),
        ),
        merchants: List<MerchantSlice> = listOf(MerchantSlice(SENTINEL_MERCHANT, 5_000_00, 3)),
        poolMinor: Long = 11_600_00,
        projected: Long = 20_000_00,
        onTrack: Boolean? = true,
    ): AnalyticsSnapshot {
        val basis = when {
            budget == null -> SafeToSpendBasis.NO_BUDGET
            poolMinor <= 0 -> SafeToSpendBasis.EXHAUSTED
            else -> SafeToSpendBasis.FULL
        }
        return AnalyticsSnapshot.empty(YearMonth.of(2026, 6)).copy(
            monthToDateMinor = monthToDate,
            overallBudgetMinor = budget,
            overallRemainingMinor = budget?.minus(monthToDate),
            overallState = BudgetState.UNDER,
            byCategory = topCategories,
            topCategories = topCategories,
            topMerchants = merchants,
            projection = Projection(
                projectedMonthEndMinor = projected,
                budgetMinor = budget,
                projectedOverBudgetMinor = budget?.let { maxOf(0L, projected - it) },
                onTrack = onTrack,
                daysElapsed = 15,
                daysRemaining = 16,
            ),
            safeToSpend = SafeToSpend(
                perDayMinor = if (poolMinor > 0) poolMinor / 16 else 0,
                remainingPoolMinor = poolMinor,
                daysRemaining = 16,
                basis = basis,
            ),
            streaks = Streaks(currentUnderBudgetDays = 3, longestUnderBudgetDays = 5),
            isEmpty = false,
        )
    }
}
