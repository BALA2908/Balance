package com.balance.budget.domain.analytics

import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.domain.model.ExpenseWithCategory
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * Pure time-series helpers for Reports. Like the engine, no Android/IO — just
 * deterministic transforms over the month's rows.
 */
object TrendAnalytics {

    /**
     * Cumulative spend (paise) for each day from the 1st through "today" (or the
     * last day of the month, for a fully past month). Index 0 = day 1.
     */
    fun dailyCumulative(
        monthExpenses: List<ExpenseWithCategory>,
        month: YearMonth,
        nowMillis: Long,
        zone: ZoneId = DateTimeUtil.zone,
    ): List<Long> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val lastDay = if (YearMonth.from(today) == month) today.dayOfMonth else month.lengthOfMonth()
        val byDay = monthExpenses
            .groupBy { Instant.ofEpochMilli(it.expense.timestamp).atZone(zone).toLocalDate().dayOfMonth }
            .mapValues { (_, rows) -> rows.sumOf { it.expense.amountMinor } }
        var cumulative = 0L
        return (1..lastDay).map { day ->
            cumulative += byDay[day] ?: 0L
            cumulative
        }
    }
}
