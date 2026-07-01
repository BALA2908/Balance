package com.balance.budget.domain.analytics

import kotlin.math.floor

/**
 * Pure calculator for a 50/30/20-style budget derived from monthly income.
 * Deterministic — no AI, no side effects:
 *   overall = income × (1 − savingsRate)
 * The optional per-category split distributes that overall across categories by
 * their share of last month's actual spend. With no spending history it returns
 * overall-only (an empty split), so the caller just sets the monthly budget.
 */
object BudgetTemplate {

    data class Result(
        val overallMinor: Long,
        val perCategoryMinor: Map<Long, Long>,
    )

    /**
     * @param incomeMinor monthly income in paise (must be > 0 to produce a budget)
     * @param savingsRatePercent portion to set aside, 0..100 (20 ⇒ the 50/30/20 rule)
     * @param prevMonthByCategory categoryId → last month's actual spend (paise)
     */
    fun fromIncome(
        incomeMinor: Long,
        savingsRatePercent: Int,
        prevMonthByCategory: Map<Long, Long> = emptyMap(),
    ): Result {
        if (incomeMinor <= 0L) return Result(0L, emptyMap())

        val rate = savingsRatePercent.coerceIn(0, 100)
        val overall = incomeMinor * (100 - rate) / 100

        val positive = prevMonthByCategory.filterValues { it > 0 }
        val total = positive.values.sum()
        if (total <= 0L || overall <= 0L) return Result(overall, emptyMap())

        // Largest-remainder apportionment so the per-category parts sum EXACTLY to
        // overall (no rounding drift). Use Double for the ratio to avoid overflow,
        // then hand out the leftover paise to the largest fractional shares.
        val ids = positive.keys.toList()
        val split = HashMap<Long, Long>(ids.size)
        for (id in ids) {
            val exact = overall.toDouble() * positive.getValue(id) / total
            split[id] = floor(exact).toLong()
        }
        var remainder = overall - split.values.sum()
        val byFraction = ids.sortedByDescending { id ->
            val exact = overall.toDouble() * positive.getValue(id) / total
            exact - floor(exact)
        }
        var i = 0
        while (remainder > 0 && byFraction.isNotEmpty()) {
            val id = byFraction[i % byFraction.size]
            split[id] = split.getValue(id) + 1
            remainder--
            i++
        }
        return Result(overall, split)
    }
}
