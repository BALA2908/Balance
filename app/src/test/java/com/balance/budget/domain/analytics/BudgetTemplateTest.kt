package com.balance.budget.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetTemplateTest {

    @Test
    fun `overall is income minus savings rate`() {
        // ₹1,00,000 income, save 20% → ₹80,000 to spend.
        val r = BudgetTemplate.fromIncome(incomeMinor = 10_000_000, savingsRatePercent = 20)
        assertEquals(8_000_000L, r.overallMinor)
        assertTrue(r.perCategoryMinor.isEmpty())
    }

    @Test
    fun `zero income yields nothing`() {
        val r = BudgetTemplate.fromIncome(incomeMinor = 0, savingsRatePercent = 20)
        assertEquals(0L, r.overallMinor)
        assertTrue(r.perCategoryMinor.isEmpty())
    }

    @Test
    fun `savings rate is clamped to 0-100`() {
        assertEquals(0L, BudgetTemplate.fromIncome(1_000_000, 150).overallMinor)      // 100% saved
        assertEquals(1_000_000L, BudgetTemplate.fromIncome(1_000_000, -10).overallMinor) // 0% saved
    }

    @Test
    fun `per-category split sums exactly to overall`() {
        val prev = mapOf(1L to 3_000_00L, 2L to 1_000_00L, 3L to 1_000_00L)
        val r = BudgetTemplate.fromIncome(
            incomeMinor = 10_000_000,
            savingsRatePercent = 20,
            prevMonthByCategory = prev,
        )
        assertEquals(8_000_000L, r.overallMinor)
        // No rounding drift — the parts add up to the overall exactly.
        assertEquals(r.overallMinor, r.perCategoryMinor.values.sum())
        // The biggest spender last month gets the biggest slice.
        val biggest = r.perCategoryMinor.maxByOrNull { it.value }!!.key
        assertEquals(1L, biggest)
    }

    @Test
    fun `split ignores zero and negative history`() {
        val prev = mapOf(1L to 5_000_00L, 2L to 0L, 3L to -100L)
        val r = BudgetTemplate.fromIncome(2_000_000, 20, prev)
        // Only category 1 had real spend, so it takes the whole overall.
        assertEquals(setOf(1L), r.perCategoryMinor.keys)
        assertEquals(r.overallMinor, r.perCategoryMinor.getValue(1L))
    }

    @Test
    fun `no history falls back to overall-only`() {
        val r = BudgetTemplate.fromIncome(2_000_000, 30, emptyMap())
        assertEquals(1_400_000L, r.overallMinor)
        assertTrue(r.perCategoryMinor.isEmpty())
    }
}
