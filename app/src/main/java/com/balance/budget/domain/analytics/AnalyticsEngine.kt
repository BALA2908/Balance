package com.balance.budget.domain.analytics

import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.domain.model.BudgetState
import com.balance.budget.domain.model.ExpenseWithCategory
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * The deterministic analytics engine. A pure function: same [AnalyticsInputs] →
 * same [AnalyticsSnapshot], no Android, no I/O, no hidden clock. This is the ONLY
 * place money math happens for insights; the AI layer is given the results as
 * pre-formatted text and never computes anything itself.
 *
 * All money is [Long] paise. Division results are rounded to the nearest paise.
 */
object AnalyticsEngine {

    /** Deltas at or under this (₹1) read as FLAT rather than up/down. */
    private const val FLAT_THRESHOLD_MINOR = 100L
    private const val ANOMALY_SIGMA = 2.0
    private const val ANOMALY_MIN_SAMPLES = 4
    private const val DEFAULT_TOP_N = 3
    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    fun compute(inputs: AnalyticsInputs): AnalyticsSnapshot {
        val zone = DateTimeUtil.zone
        val today = Instant.ofEpochMilli(inputs.nowMillis).atZone(zone).toLocalDate()
        val month = YearMonth.from(today)
        val daysInMonth = month.lengthOfMonth()
        val daysElapsed = today.dayOfMonth                       // includes today
        val daysRemaining = daysInMonth - daysElapsed + 1        // includes today

        val mtd = sumOf(inputs.monthExpenses)

        val byCategory = byCategory(inputs, mtd)
        val topCategories = byCategory.filter { it.spentMinor > 0 }.take(DEFAULT_TOP_N)
        val topMerchants = topMerchants(inputs.monthExpenses, DEFAULT_TOP_N)

        val dailyAverage = divRound(mtd, daysElapsed)
        val weeksElapsed = max(1, ceil(daysElapsed / 7.0).toInt())
        val weeklyAverage = divRound(mtd, weeksElapsed)

        // WoW windows can straddle the month boundary, so consider both months' rows.
        val recent = inputs.monthExpenses + inputs.prevMonthExpenses
        val weekOverWeek = weekOverWeek(recent, inputs.nowMillis)

        val prevToSameDay = prevMonthToSameDay(inputs.prevMonthExpenses, today, zone)
        val monthOverMonth = trendOf(mtd, prevToSameDay)

        // Headline "rolled over" = last month's overall leftover, positive-only.
        val rolloverCarry = if (inputs.rolloverEnabled && inputs.prevOverallBudgetMinor != null) {
            max(0L, inputs.prevOverallBudgetMinor - sumOf(inputs.prevMonthExpenses))
        } else 0L

        // Envelope (zero-based) mode: the spendable pool is the sum of category
        // envelopes (which already include per-category carry + moves), so we don't
        // add the overall carry again. Default off → unchanged behavior.
        val envelopeSum = byCategory.sumOf { it.effectiveBudgetMinor ?: 0L }
        val useEnvelope = inputs.envelopeMode && envelopeSum > 0L
        val stsBudget = if (useEnvelope) envelopeSum else inputs.overallBudgetMinor
        val stsCarry = if (useEnvelope) 0L else rolloverCarry

        val projection = projectMonthEnd(mtd, daysElapsed, daysInMonth, inputs.overallBudgetMinor, daysRemaining)
        val safeToSpend = safeToSpend(
            overallBudget = stsBudget,
            mtd = mtd,
            recurringTotal = inputs.activeRecurringTotalMinor,
            recurringPaid = inputs.recurringPaidThisMonthMinor,
            daysRemaining = daysRemaining,
            rolloverCarry = stsCarry,
        )
        val streaks = streaks(inputs.monthExpenses, inputs.overallBudgetMinor, today, month, zone)
        val anomalies = anomalies(inputs.monthExpenses)

        val snapshot = AnalyticsSnapshot(
            month = month,
            monthToDateMinor = mtd,
            overallBudgetMinor = inputs.overallBudgetMinor,
            overallRemainingMinor = inputs.overallBudgetMinor?.let { it - mtd },
            overallState = BudgetState.of(mtd, inputs.overallBudgetMinor),
            byCategory = byCategory,
            topCategories = topCategories,
            topMerchants = topMerchants,
            dailyAverageMinor = dailyAverage,
            weeklyAverageMinor = weeklyAverage,
            weekOverWeek = weekOverWeek,
            monthOverMonth = monthOverMonth,
            projection = projection,
            safeToSpend = safeToSpend,
            streaks = streaks,
            anomalies = anomalies,
            isEmpty = inputs.monthExpenses.isEmpty(),
            rolloverCarryMinor = rolloverCarry,
            activeRecurringTotalMinor = inputs.activeRecurringTotalMinor,
        )
        // Financial-health reads from the assembled snapshot (+ optional income).
        return snapshot.copy(financialHealth = FinancialHealth.from(snapshot, inputs.monthlyIncomeMinor))
    }

    // --- pieces (internal so they can be unit-tested directly) ----------------

    internal fun sumOf(rows: List<ExpenseWithCategory>): Long =
        rows.sumOf { it.expense.amountMinor }

    /** Rounds total/divisor to the nearest paise; 0 when divisor <= 0. */
    internal fun divRound(total: Long, divisor: Int): Long =
        if (divisor <= 0) 0L else (total.toDouble() / divisor).roundToLong()

    internal fun byCategory(inputs: AnalyticsInputs, mtd: Long): List<CategorySlice> {
        val byId = inputs.categories.associateBy { it.id }
        val catFromRows = inputs.monthExpenses.associate { it.expense.categoryId to it.category }
        val spentById = inputs.monthExpenses
            .groupBy { it.expense.categoryId }
            .mapValues { (_, rows) -> rows.sumOf { it.expense.amountMinor } }
        // Last month's actual spend per category — the basis for positive carry.
        val prevActualById = inputs.prevMonthExpenses
            .groupBy { it.expense.categoryId }
            .mapValues { (_, rows) -> rows.sumOf { it.expense.amountMinor } }

        // Include every category that has spend OR a per-category budget.
        val ids = spentById.keys + inputs.categoryBudgetsMinor.keys
        return ids.mapNotNull { id ->
            val cat = byId[id] ?: catFromRows[id] ?: return@mapNotNull null
            val spent = spentById[id] ?: 0L
            val budget = inputs.categoryBudgetsMinor[id]

            // Positive carry: only what was BUDGETED-but-unspent last month, and only
            // when rollover is on. Overspend never carries forward as a penalty.
            val carryIn = if (inputs.rolloverEnabled) {
                inputs.prevCategoryBudgetsMinor[id]
                    ?.let { prevBudget -> max(0L, prevBudget - (prevActualById[id] ?: 0L)) }
                    ?: 0L
            } else 0L

            // Net reallocation this month (independent of the rollover toggle —
            // a move is an explicit user action for the current month).
            val movedIn = inputs.monthAdjustments.filter { it.toCategoryId == id }.sumOf { it.amountMinor }
            val movedOut = inputs.monthAdjustments.filter { it.fromCategoryId == id }.sumOf { it.amountMinor }
            val adjustment = movedIn - movedOut

            val effective = budget?.let { it + carryIn + adjustment }
            CategorySlice(
                categoryId = id,
                name = cat.name,
                colorHex = cat.colorHex,
                iconKey = cat.iconKey,
                spentMinor = spent,
                percentOfTotal = if (mtd > 0) spent * 100.0 / mtd else 0.0,
                budgetMinor = budget,
                remainingMinor = effective?.let { it - spent },
                isOverBudget = effective != null && spent > effective,
                state = BudgetState.of(spent, effective),
                carryInMinor = carryIn,
                adjustmentMinor = adjustment,
                effectiveBudgetMinor = effective,
            )
        }.sortedByDescending { it.spentMinor }
    }

    internal fun topMerchants(rows: List<ExpenseWithCategory>, limit: Int): List<MerchantSlice> =
        rows.filter { !it.expense.merchant.isNullOrBlank() }
            .groupBy { it.expense.merchant!!.trim() }
            .map { (m, rs) -> MerchantSlice(m, rs.sumOf { it.expense.amountMinor }, rs.size) }
            .sortedByDescending { it.spentMinor }
            .take(limit)

    internal fun weekOverWeek(rows: List<ExpenseWithCategory>, nowMillis: Long): Trend {
        val cut7 = nowMillis - 7 * MILLIS_PER_DAY
        val cut14 = nowMillis - 14 * MILLIS_PER_DAY
        val cur = rows.filter { it.expense.timestamp in (cut7 + 1)..nowMillis }
            .sumOf { it.expense.amountMinor }
        val prev = rows.filter { it.expense.timestamp in (cut14 + 1)..cut7 }
            .sumOf { it.expense.amountMinor }
        return trendOf(cur, prev)
    }

    /** Previous month's spend up to the same day-of-month — apples to apples. */
    internal fun prevMonthToSameDay(
        prevRows: List<ExpenseWithCategory>,
        today: LocalDate,
        zone: ZoneId,
    ): Long {
        val prevMonth = YearMonth.from(today).minusMonths(1)
        val cutoffDay = min(today.dayOfMonth, prevMonth.lengthOfMonth())
        return prevRows.filter { row ->
            Instant.ofEpochMilli(row.expense.timestamp).atZone(zone).toLocalDate().dayOfMonth <= cutoffDay
        }.sumOf { it.expense.amountMinor }
    }

    internal fun projectMonthEnd(
        mtd: Long,
        daysElapsed: Int,
        daysInMonth: Int,
        budget: Long?,
        daysRemaining: Int,
    ): Projection {
        val projected = if (daysElapsed <= 0) mtd else divRound(mtd * daysInMonth, daysElapsed)
        return Projection(
            projectedMonthEndMinor = projected,
            budgetMinor = budget,
            projectedOverBudgetMinor = budget?.let { max(0L, projected - it) },
            onTrack = budget?.let { projected <= it },
            daysElapsed = daysElapsed,
            daysRemaining = daysRemaining,
        )
    }

    /**
     * Safe-to-spend. The reserve subtracts only the UNPAID portion of recurring
     * commitments, because any recurring already posted this month is already
     * inside [mtd] (its rows have source = RECURRING) — never double-counted.
     */
    internal fun safeToSpend(
        overallBudget: Long?,
        mtd: Long,
        recurringTotal: Long,
        recurringPaid: Long,
        daysRemaining: Int,
        rolloverCarry: Long = 0L,
    ): SafeToSpend {
        if (overallBudget == null) {
            return SafeToSpend(0, 0, daysRemaining, SafeToSpendBasis.NO_BUDGET)
        }
        val unpaidRecurring = max(0L, recurringTotal - recurringPaid)
        // A great last month visibly *adds* to today's pool when rollover is on.
        val pool = overallBudget + rolloverCarry - mtd - unpaidRecurring
        return if (pool <= 0L) {
            SafeToSpend(0, pool, daysRemaining, SafeToSpendBasis.EXHAUSTED)
        } else {
            SafeToSpend(divRound(pool, daysRemaining), pool, daysRemaining, SafeToSpendBasis.FULL)
        }
    }

    /**
     * Consecutive "days under budget" ending yesterday (today is still in
     * progress, so it's excluded). A daily budget = monthly / days-in-month.
     */
    internal fun streaks(
        rows: List<ExpenseWithCategory>,
        overallBudget: Long?,
        today: LocalDate,
        month: YearMonth,
        zone: ZoneId,
    ): Streaks {
        if (overallBudget == null || overallBudget <= 0L) return Streaks(0, 0)
        val dailyBudget = divRound(overallBudget, month.lengthOfMonth())
        val spendByDay = rows.groupBy {
            Instant.ofEpochMilli(it.expense.timestamp).atZone(zone).toLocalDate().dayOfMonth
        }.mapValues { (_, rs) -> rs.sumOf { it.expense.amountMinor } }

        var run = 0
        var longest = 0
        var underDays = 0
        val completedDays = today.dayOfMonth - 1
        for (day in 1 until today.dayOfMonth) { // completed days only
            val spend = spendByDay[day] ?: 0L
            if (spend <= dailyBudget) {
                run++
                longest = max(longest, run)
                underDays++
            } else {
                run = 0
            }
        }
        val adherence = if (completedDays > 0) underDays * 100.0 / completedDays else 0.0
        return Streaks(currentUnderBudgetDays = run, longestUnderBudgetDays = longest, adherencePercent = adherence)
    }

    /** Flags expenses above mean + 2σ within their category (needs ≥4 samples). */
    internal fun anomalies(rows: List<ExpenseWithCategory>): List<Anomaly> {
        val byCat = rows.groupBy { it.expense.categoryId }
        val out = mutableListOf<Anomaly>()
        for ((_, catRows) in byCat) {
            if (catRows.size < ANOMALY_MIN_SAMPLES) continue
            val amounts = catRows.map { it.expense.amountMinor }
            val mean = amounts.average()
            val variance = amounts.map { (it - mean) * (it - mean) }.average()
            val std = sqrt(variance)
            if (std <= 0.0) continue
            val threshold = mean + ANOMALY_SIGMA * std
            for (row in catRows) {
                val amt = row.expense.amountMinor
                if (amt > threshold) {
                    out += Anomaly(
                        expenseId = row.expense.id,
                        categoryId = row.expense.categoryId,
                        categoryName = row.category.name,
                        amountMinor = amt,
                        categoryMeanMinor = mean.roundToLong(),
                        categoryStdDevMinor = std.roundToLong(),
                        sigmaOver = (amt - mean) / std,
                    )
                }
            }
        }
        return out.sortedByDescending { it.sigmaOver }
    }

    private fun trendOf(current: Long, previous: Long): Trend {
        val delta = current - previous
        val direction = when {
            abs(delta) <= FLAT_THRESHOLD_MINOR -> TrendDirection.FLAT
            delta > 0 -> TrendDirection.UP
            else -> TrendDirection.DOWN
        }
        return Trend(
            currentMinor = current,
            previousMinor = previous,
            deltaMinor = delta,
            percentChange = if (previous == 0L) null else delta * 100.0 / previous,
            direction = direction,
        )
    }
}
