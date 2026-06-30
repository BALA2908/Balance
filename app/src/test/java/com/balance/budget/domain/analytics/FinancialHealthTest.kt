package com.balance.budget.domain.analytics

import com.balance.budget.domain.model.BudgetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class FinancialHealthTest {

    private val month = YearMonth.of(2026, 6)

    private fun slice(icon: String, spent: Long) = CategorySlice(
        categoryId = icon.hashCode().toLong(),
        name = icon,
        colorHex = "#000000",
        iconKey = icon,
        spentMinor = spent,
        percentOfTotal = 0.0,
        budgetMinor = null,
        remainingMinor = null,
        isOverBudget = false,
        state = BudgetState.UNDER,
    )

    private fun snap(
        mtd: Long,
        slices: List<CategorySlice> = emptyList(),
        projected: Long = mtd,
        onTrack: Boolean? = null,
        adherence: Double = 0.0,
        recurring: Long = 0,
        budget: Long? = null,
    ) = AnalyticsSnapshot.empty(month).copy(
        monthToDateMinor = mtd,
        overallBudgetMinor = budget,
        byCategory = slices,
        projection = Projection(projected, budget, budget?.let { maxOf(0L, projected - it) }, onTrack, 15, 16),
        streaks = Streaks(0, 0, adherence),
        activeRecurringTotalMinor = recurring,
        isEmpty = mtd == 0L && slices.isEmpty(),
    )

    @Test fun `savings rate uses projected month-end vs income`() {
        // income 50,000; projected month-end 40,000 -> save 20%.
        val h = FinancialHealth.from(snap(mtd = 20_000_00, projected = 40_000_00), incomeMinor = 50_000_00)!!
        assertEquals(20.0, h.savingsRatePercent!!, 0.5)
        assertEquals(30_000_00L, h.savedSoFarMinor) // 50,000 - 20,000 spent so far
    }

    @Test fun `no income hides savings + recurring metrics`() {
        val h = FinancialHealth.from(snap(mtd = 10_000_00), incomeMinor = null)!!
        assertNull(h.savingsRatePercent)
        assertNull(h.savedSoFarMinor)
        assertNull(h.recurringBurdenPercent)
    }

    @Test fun `investment share is of total spend`() {
        val h = FinancialHealth.from(
            snap(mtd = 10_000_00, slices = listOf(slice("food", 8_000_00), slice("investment", 2_000_00))),
            incomeMinor = null,
        )!!
        assertEquals(20.0, h.investmentSharePercent, 0.1)
    }

    @Test fun `discipline score stays within floored 35-100 band`() {
        val low = FinancialHealth.from(snap(mtd = 10_000_00, projected = 99_000_00, onTrack = false, budget = 5_000_00), incomeMinor = 10_000_00)!!
        val high = FinancialHealth.from(snap(mtd = 4_000_00, projected = 8_000_00, onTrack = true, adherence = 95.0, budget = 30_000_00, slices = listOf(slice("investment", 1_500_00))), incomeMinor = 50_000_00)!!
        assertTrue(low.disciplineScore in 35..100)
        assertTrue(high.disciplineScore in 35..100)
        assertTrue("healthy month should score higher", high.disciplineScore > low.disciplineScore)
    }

    @Test fun `low investment yields an investing recommendation`() {
        val h = FinancialHealth.from(snap(mtd = 10_000_00, slices = listOf(slice("food", 10_000_00))), incomeMinor = null)!!
        assertTrue(h.recommendations.isNotEmpty())
        assertTrue(h.recommendations.any { it.contains("SIP", ignoreCase = true) || it.contains("invest", ignoreCase = true) })
    }

    @Test fun `empty month with no income has no health`() {
        assertNull(FinancialHealth.from(snap(mtd = 0L), incomeMinor = null))
    }
}
