package com.balance.budget.domain.analytics

import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.domain.model.Category
import com.balance.budget.domain.model.Expense
import com.balance.budget.domain.model.ExpenseSource
import com.balance.budget.domain.model.ExpenseWithCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure, table-driven tests for the analytics engine. A fixed "now" of
 * 2026-06-15 (day 15 of a 30-day month) gives daysElapsed = 15, daysRemaining = 16.
 * Timestamps are built through DateTimeUtil so the test and the engine resolve
 * the same local zone on any machine.
 */
class AnalyticsEngineTest {

    private val month = YearMonth.of(2026, 6)
    private val zone = DateTimeUtil.zone

    private val food = Category(1, "Food", "food", "#E0795B", true, false, 0)
    private val travel = Category(2, "Travel", "travel", "#6FA8A0", true, false, 1)
    private val bills = Category(3, "Bills", "bills", "#7E97C9", true, false, 2)
    private val cats = listOf(food, travel, bills)

    private fun millis(day: Int, hour: Int = 12): Long =
        LocalDate.of(2026, 6, day).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private val now = millis(15) // 2026-06-15 noon

    private var nextId = 1L
    private fun ex(
        amountMinor: Long,
        cat: Category,
        day: Int,
        source: ExpenseSource = ExpenseSource.MANUAL,
        merchant: String? = null,
    ): ExpenseWithCategory {
        val id = nextId++
        val ts = millis(day)
        return ExpenseWithCategory(
            expense = Expense(id, amountMinor, cat.id, null, ts, ts, source, merchant),
            category = cat,
        )
    }

    /** A previous-calendar-month expense (for carry / month-over-month tests). */
    private fun prevEx(amountMinor: Long, cat: Category, day: Int): ExpenseWithCategory {
        val id = nextId++
        val ts = prevMonthMillis(day)
        return ExpenseWithCategory(
            expense = Expense(id, amountMinor, cat.id, null, ts, ts, ExpenseSource.MANUAL, null),
            category = cat,
        )
    }

    private fun inputs(
        monthExpenses: List<ExpenseWithCategory> = emptyList(),
        prevMonthExpenses: List<ExpenseWithCategory> = emptyList(),
        overallBudget: Long? = null,
        categoryBudgets: Map<Long, Long> = emptyMap(),
        recurringTotal: Long = 0,
        recurringPaid: Long = 0,
        nowMillis: Long = now,
        rolloverEnabled: Boolean = false,
        prevOverallBudget: Long? = null,
        prevCategoryBudgets: Map<Long, Long> = emptyMap(),
        monthAdjustments: List<BudgetAdjustment> = emptyList(),
        envelopeMode: Boolean = false,
    ) = AnalyticsInputs(
        nowMillis = nowMillis,
        monthExpenses = monthExpenses,
        prevMonthExpenses = prevMonthExpenses,
        categories = cats,
        overallBudgetMinor = overallBudget,
        categoryBudgetsMinor = categoryBudgets,
        activeRecurringTotalMinor = recurringTotal,
        recurringPaidThisMonthMinor = recurringPaid,
        rolloverEnabled = rolloverEnabled,
        prevOverallBudgetMinor = prevOverallBudget,
        prevCategoryBudgetsMinor = prevCategoryBudgets,
        monthAdjustments = monthAdjustments,
        envelopeMode = envelopeMode,
    )

    @Test fun `empty month is empty with no-budget safe-to-spend`() {
        val s = AnalyticsEngine.compute(inputs())
        assertTrue(s.isEmpty)
        assertEquals(0L, s.monthToDateMinor)
        assertEquals(SafeToSpendBasis.NO_BUDGET, s.safeToSpend.basis)
        assertEquals(0L, s.safeToSpend.perDayMinor)
        assertEquals(16, s.safeToSpend.daysRemaining)
    }

    @Test fun `month-to-date sums all month expenses`() {
        val s = AnalyticsEngine.compute(
            inputs(monthExpenses = listOf(ex(6_000_00, food, 3), ex(4_000_00, travel, 10)))
        )
        assertEquals(10_000_00L, s.monthToDateMinor)
        assertFalse(s.isEmpty)
    }

    @Test fun `category breakdown computes share and includes budgeted-but-zero`() {
        val s = AnalyticsEngine.compute(
            inputs(
                monthExpenses = listOf(ex(6_000_00, food, 3), ex(4_000_00, travel, 10)),
                categoryBudgets = mapOf(bills.id to 2_000_00), // Bills budgeted, zero spend
            )
        )
        val byId = s.byCategory.associateBy { it.categoryId }
        assertEquals(60.0, byId.getValue(food.id).percentOfTotal, 0.001)
        assertEquals(40.0, byId.getValue(travel.id).percentOfTotal, 0.001)
        // Bills appears despite zero spend, with its budget and not over.
        val billsSlice = byId.getValue(bills.id)
        assertEquals(0L, billsSlice.spentMinor)
        assertEquals(2_000_00L, billsSlice.budgetMinor)
        assertFalse(billsSlice.isOverBudget)
        // Top categories only include spend > 0.
        assertEquals(listOf(food.id, travel.id), s.topCategories.map { it.categoryId })
    }

    @Test fun `daily and weekly averages round to nearest paise`() {
        // mtd 10,000 over 15 elapsed days -> 666.67/day; 3 weeks elapsed -> 3,333.33/week
        val s = AnalyticsEngine.compute(inputs(monthExpenses = listOf(ex(10_000_00, food, 5))))
        assertEquals(666_67L, s.dailyAverageMinor)   // round(1000000/15)
        assertEquals(3_333_33L, s.weeklyAverageMinor) // round(1000000/3)
    }

    @Test fun `month-end projection extrapolates from pace`() {
        // day 15 of 30, mtd 15,000 -> projected 30,000
        val s = AnalyticsEngine.compute(
            inputs(monthExpenses = listOf(ex(15_000_00, food, 7)), overallBudget = 28_000_00)
        )
        assertEquals(30_000_00L, s.projection.projectedMonthEndMinor)
        assertEquals(15, s.projection.daysElapsed)
        assertEquals(16, s.projection.daysRemaining)
        assertEquals(2_000_00L, s.projection.projectedOverBudgetMinor)
        assertFalse(s.projection.onTrack!!)
    }

    @Test fun `safe-to-spend reserves only unpaid recurring`() {
        // budget 30,000; spent 18,400; recurring 10,000 NOT yet paid -> reserve 10,000
        val unpaid = AnalyticsEngine.compute(
            inputs(
                monthExpenses = listOf(ex(18_400_00, food, 4)),
                overallBudget = 30_000_00,
                recurringTotal = 10_000_00,
                recurringPaid = 0,
            )
        ).safeToSpend
        // pool = 30000 - 18400 - 10000 = 1,600 ; /16 days = 100/day
        assertEquals(SafeToSpendBasis.FULL, unpaid.basis)
        assertEquals(1_600_00L, unpaid.remainingPoolMinor)
        assertEquals(100_00L, unpaid.perDayMinor)
    }

    @Test fun `safe-to-spend does not double count already-paid recurring`() {
        // Same budget/spend, but the 10,000 recurring is already inside mtd (paid).
        val paid = AnalyticsEngine.compute(
            inputs(
                monthExpenses = listOf(ex(18_400_00, food, 4)),
                overallBudget = 30_000_00,
                recurringTotal = 10_000_00,
                recurringPaid = 10_000_00,
            )
        ).safeToSpend
        // pool = 30000 - 18400 - max(0, 10000-10000) = 11,600 (NOT 1,600)
        assertEquals(11_600_00L, paid.remainingPoolMinor)
        assertEquals(725_00L, paid.perDayMinor) // 1160000/16
    }

    @Test fun `safe-to-spend is exhausted when over budget`() {
        val s = AnalyticsEngine.compute(
            inputs(monthExpenses = listOf(ex(31_000_00, food, 4)), overallBudget = 30_000_00)
        ).safeToSpend
        assertEquals(SafeToSpendBasis.EXHAUSTED, s.basis)
        assertEquals(0L, s.perDayMinor)
    }

    @Test fun `week over week compares trailing 7-day windows`() {
        // cur window (June 8 noon, June 15 noon]: day 14; prev (June 1 noon, June 8 noon]: day 3
        val s = AnalyticsEngine.compute(
            inputs(monthExpenses = listOf(ex(1_000_00, food, 14), ex(500_00, food, 3)))
        )
        assertEquals(1_000_00L, s.weekOverWeek.currentMinor)
        assertEquals(500_00L, s.weekOverWeek.previousMinor)
        assertEquals(TrendDirection.UP, s.weekOverWeek.direction)
        assertEquals(100.0, s.weekOverWeek.percentChange!!, 0.001)
    }

    @Test fun `month over month uses previous month up to same day`() {
        // prev month: day 10 counts (<=15), day 28 does not
        val s = AnalyticsEngine.compute(
            inputs(
                monthExpenses = listOf(ex(5_000_00, food, 5)),
                prevMonthExpenses = listOf(
                    ExpenseWithCategory(
                        Expense(100, 3_000_00, food.id, null, prevMonthMillis(10), prevMonthMillis(10), ExpenseSource.MANUAL, null),
                        food,
                    ),
                    ExpenseWithCategory(
                        Expense(101, 9_000_00, food.id, null, prevMonthMillis(28), prevMonthMillis(28), ExpenseSource.MANUAL, null),
                        food,
                    ),
                ),
            )
        )
        assertEquals(5_000_00L, s.monthOverMonth.currentMinor)
        assertEquals(3_000_00L, s.monthOverMonth.previousMinor) // only day-10 row counts
    }

    @Test fun `streak counts consecutive under-budget days up to yesterday`() {
        // daily budget = 30,000 / 30 = 1,000. Days 1..14 completed (today=15).
        // Put an over-budget day on day 5 (2,000) so the streak resets there.
        val rows = buildList {
            add(ex(2_000_00, food, 5))   // over the 1,000 daily budget -> breaks streak
            add(ex(200_00, food, 9))     // under
        }
        val s = AnalyticsEngine.compute(inputs(monthExpenses = rows, overallBudget = 30_000_00))
        // Days 6..14 are all <= 1,000 (only day 9 has 200) -> current run = 9 (days 6..14)
        assertEquals(9, s.streaks.currentUnderBudgetDays)
        assertTrue(s.streaks.longestUnderBudgetDays >= 9)
    }

    @Test fun `anomaly flags large outlier only with enough samples`() {
        // 5 small + 1 huge in Food. With population std the max z-score is
        // sqrt(n-1), so a single outlier can only clear mean+2sd at n>=6.
        val withEnough = AnalyticsEngine.compute(
            inputs(
                monthExpenses = listOf(
                    ex(100_00, food, 2), ex(120_00, food, 3),
                    ex(110_00, food, 4), ex(130_00, food, 6),
                    ex(105_00, food, 7), ex(5_000_00, food, 8),
                )
            )
        )
        assertTrue(withEnough.anomalies.any { it.amountMinor == 5_000_00L })

        // Same outlier but only 2 samples total -> below ANOMALY_MIN_SAMPLES, no flag
        nextId = 1
        val tooFew = AnalyticsEngine.compute(
            inputs(monthExpenses = listOf(ex(100_00, food, 2), ex(5_000_00, food, 8)))
        )
        assertTrue(tooFew.anomalies.isEmpty())
    }

    @Test fun `no overall budget yields under state and null remaining`() {
        val s = AnalyticsEngine.compute(inputs(monthExpenses = listOf(ex(1_000_00, food, 3))))
        assertNull(s.overallRemainingMinor)
        assertNull(s.projection.onTrack)
    }

    // --- Rollover (Wave 6) -----------------------------------------------------

    @Test fun `rollover off leaves carry zero and safe-to-spend unchanged`() {
        // Even with prev budgets present, nothing carries while the flag is off.
        val s = AnalyticsEngine.compute(
            inputs(
                monthExpenses = listOf(ex(18_400_00, food, 4)),
                overallBudget = 30_000_00,
                categoryBudgets = mapOf(food.id to 10_000_00),
                prevOverallBudget = 30_000_00,
                prevCategoryBudgets = mapOf(food.id to 10_000_00),
                prevMonthExpenses = listOf(prevEx(2_000_00, food, 5)),
                rolloverEnabled = false,
            )
        )
        assertEquals(0L, s.rolloverCarryMinor)
        val foodSlice = s.byCategory.first { it.categoryId == food.id }
        assertEquals(0L, foodSlice.carryInMinor)
        assertEquals(10_000_00L, foodSlice.effectiveBudgetMinor)
        // pool = 30000 - 18400 = 11,600 (no carry added)
        assertEquals(11_600_00L, s.safeToSpend.remainingPoolMinor)
    }

    @Test fun `positive category carry adds to effective budget`() {
        // Food budgeted 10,000 last month, spent only 6,000 -> carry 4,000.
        val s = AnalyticsEngine.compute(
            inputs(
                monthExpenses = listOf(ex(3_000_00, food, 4)),
                categoryBudgets = mapOf(food.id to 8_000_00),
                prevCategoryBudgets = mapOf(food.id to 10_000_00),
                prevMonthExpenses = listOf(prevEx(6_000_00, food, 5)),
                rolloverEnabled = true,
            )
        )
        val foodSlice = s.byCategory.first { it.categoryId == food.id }
        assertEquals(4_000_00L, foodSlice.carryInMinor)
        assertEquals(12_000_00L, foodSlice.effectiveBudgetMinor) // 8,000 + 4,000 carry
        assertEquals(9_000_00L, foodSlice.remainingMinor)        // 12,000 - 3,000
        assertFalse(foodSlice.isOverBudget)
    }

    @Test fun `carry is positive-only — overspending last month never penalises`() {
        // Food overspent last month (budget 5,000, spent 9,000) -> carry must be 0.
        val s = AnalyticsEngine.compute(
            inputs(
                monthExpenses = listOf(ex(1_000_00, food, 4)),
                categoryBudgets = mapOf(food.id to 5_000_00),
                prevCategoryBudgets = mapOf(food.id to 5_000_00),
                prevMonthExpenses = listOf(prevEx(9_000_00, food, 5)),
                rolloverEnabled = true,
            )
        )
        val foodSlice = s.byCategory.first { it.categoryId == food.id }
        assertEquals(0L, foodSlice.carryInMinor)
        assertEquals(5_000_00L, foodSlice.effectiveBudgetMinor)
    }

    @Test fun `overall leftover rolls into safe-to-spend pool`() {
        // Prev overall budget 30,000, spent 28,760 -> 1,240 rolls over.
        val s = AnalyticsEngine.compute(
            inputs(
                monthExpenses = listOf(ex(10_000_00, food, 4)),
                overallBudget = 30_000_00,
                prevOverallBudget = 30_000_00,
                prevMonthExpenses = listOf(prevEx(28_760_00, food, 5)),
                rolloverEnabled = true,
            )
        )
        assertEquals(1_240_00L, s.rolloverCarryMinor)
        // pool = 30,000 + 1,240 - 10,000 = 21,240
        assertEquals(21_240_00L, s.safeToSpend.remainingPoolMinor)
    }

    @Test fun `move budget reshapes envelopes but nets zero on the overall pool`() {
        // Move 2,000 from Food to Travel this month.
        val s = AnalyticsEngine.compute(
            inputs(
                monthExpenses = listOf(ex(1_000_00, food, 4)),
                overallBudget = 30_000_00,
                categoryBudgets = mapOf(food.id to 10_000_00, travel.id to 5_000_00),
                monthAdjustments = listOf(BudgetAdjustment(food.id, travel.id, 2_000_00)),
            )
        )
        val byId = s.byCategory.associateBy { it.categoryId }
        assertEquals(-2_000_00L, byId.getValue(food.id).adjustmentMinor)
        assertEquals(8_000_00L, byId.getValue(food.id).effectiveBudgetMinor)
        assertEquals(2_000_00L, byId.getValue(travel.id).adjustmentMinor)
        assertEquals(7_000_00L, byId.getValue(travel.id).effectiveBudgetMinor)
        // Reallocation doesn't change overall safe-to-spend: 30,000 - 1,000 = 29,000.
        assertEquals(29_000_00L, s.safeToSpend.remainingPoolMinor)
    }

    @Test fun `carry only applies to categories that had a previous budget`() {
        // Travel spent last month but had no prev budget -> no carry for Travel.
        val s = AnalyticsEngine.compute(
            inputs(
                monthExpenses = listOf(ex(500_00, travel, 4)),
                categoryBudgets = mapOf(travel.id to 5_000_00),
                prevCategoryBudgets = mapOf(food.id to 10_000_00), // only Food had one
                prevMonthExpenses = listOf(prevEx(3_000_00, travel, 5)),
                rolloverEnabled = true,
            )
        )
        val travelSlice = s.byCategory.first { it.categoryId == travel.id }
        assertEquals(0L, travelSlice.carryInMinor)
        assertEquals(5_000_00L, travelSlice.effectiveBudgetMinor)
    }

    @Test fun `envelope mode bases safe-to-spend on the sum of category envelopes`() {
        // Overall budget 30,000 but envelopes sum to 15,000 (Food 10k + Travel 5k).
        val s = AnalyticsEngine.compute(
            inputs(
                monthExpenses = listOf(ex(1_000_00, food, 4)),
                overallBudget = 30_000_00,
                categoryBudgets = mapOf(food.id to 10_000_00, travel.id to 5_000_00),
                envelopeMode = true,
            )
        ).safeToSpend
        // pool = 15,000 (envelopes) - 1,000 spent = 14,000 (NOT 29,000 from the overall budget)
        assertEquals(14_000_00L, s.remainingPoolMinor)
    }

    @Test fun `envelope mode off keeps overall-budget safe-to-spend`() {
        val s = AnalyticsEngine.compute(
            inputs(
                monthExpenses = listOf(ex(1_000_00, food, 4)),
                overallBudget = 30_000_00,
                categoryBudgets = mapOf(food.id to 10_000_00, travel.id to 5_000_00),
                envelopeMode = false,
            )
        ).safeToSpend
        assertEquals(29_000_00L, s.remainingPoolMinor)
    }

    private fun prevMonthMillis(day: Int): Long =
        LocalDate.of(2026, 5, day).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
}
