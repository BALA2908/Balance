package com.balance.budget.domain.ai

import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.core.util.Money
import com.balance.budget.domain.analytics.AnalyticsSnapshot
import com.balance.budget.domain.analytics.TrendDirection
import com.balance.budget.domain.model.BudgetState
import kotlin.math.roundToInt

/**
 * Builds the fact blocks handed to an [AiProvider]. CRITICAL privacy contract:
 * these strings contain ONLY pre-aggregated, anonymized numbers and category
 * NAMES — never raw expenses, notes, or merchant names. That's what makes it safe
 * to (optionally, opt-in) send to a cloud model. `PromptBuilderTest` enforces it.
 *
 * Every number here is already computed by the deterministic engine; the AI is
 * only asked to phrase them.
 */
object PromptBuilder {

    private fun pct(p: Double) = p.roundToInt()
    private fun trendWord(direction: TrendDirection, percent: Double?): String = when (direction) {
        TrendDirection.UP -> "up" + (percent?.let { " ${pct(it)}%" } ?: "")
        TrendDirection.DOWN -> "down" + (percent?.let { " ${pct(-it)}%" } ?: "")
        TrendDirection.FLAT -> "about the same"
    }

    /** Facts for the monthly "money story" summary (Analyst role). */
    fun analystFacts(s: AnalyticsSnapshot): String = buildString {
        appendLine("Month: ${DateTimeUtil.monthLabel(s.month)} (so far).")
        val budget = s.overallBudgetMinor
        if (budget != null) {
            val remaining = s.overallRemainingMinor ?: (budget - s.monthToDateMinor)
            appendLine("Spent ${Money.formatWhole(s.monthToDateMinor)} of ${Money.formatWhole(budget)} budget (${Money.formatWhole(remaining)} left).")
        } else {
            appendLine("Spent ${Money.formatWhole(s.monthToDateMinor)} (no budget set).")
        }
        if (s.topCategories.isNotEmpty()) {
            val top = s.topCategories.joinToString("; ") { "${it.name} ${Money.formatWhole(it.spentMinor)} (${pct(it.percentOfTotal)}%)" }
            appendLine("Top categories: $top.")
        }
        appendLine("Vs last month at this point: ${trendWord(s.monthOverMonth.direction, s.monthOverMonth.percentChange)}.")
        val proj = s.projection
        val projLine = when {
            proj.onTrack == true -> "Projected month-end ${Money.formatWhole(proj.projectedMonthEndMinor)} (on track)."
            (proj.projectedOverBudgetMinor ?: 0) > 0 -> "Projected month-end ${Money.formatWhole(proj.projectedMonthEndMinor)} (${Money.formatWhole(proj.projectedOverBudgetMinor!!)} over budget)."
            else -> "Projected month-end ${Money.formatWhole(proj.projectedMonthEndMinor)}."
        }
        appendLine(projLine)
    }.trim()

    /** Signals for friendly savings tips (Advisor role). */
    fun advisorFacts(s: AnalyticsSnapshot): String = buildString {
        appendLine("Safe to spend per day: ${Money.formatWhole(s.safeToSpend.perDayMinor)} (basis: ${s.safeToSpend.basis}).")
        val over = s.byCategory.filter { it.isOverBudget }
        if (over.isNotEmpty()) appendLine("Over budget in: " + over.joinToString(", ") { it.name } + ".")
        val near = s.byCategory.filter { it.budgetMinor != null && it.state == BudgetState.APPROACHING }
        if (near.isNotEmpty()) appendLine("Approaching limit in: " + near.joinToString(", ") { it.name } + ".")
        appendLine("On track this month: ${s.projection.onTrack ?: "no budget"}.")
        appendLine("Current under-budget streak: ${s.streaks.currentUnderBudgetDays} days.")
    }.trim()

    /** Facts for the forecast line (Forecaster role). */
    fun forecastFacts(s: AnalyticsSnapshot): String = buildString {
        val proj = s.projection
        appendLine("Projected month-end: ${Money.formatWhole(proj.projectedMonthEndMinor)}.")
        s.overallBudgetMinor?.let { appendLine("Budget: ${Money.formatWhole(it)}.") }
        appendLine("Days left (incl. today): ${proj.daysRemaining}.")
        appendLine("Safe to spend per day: ${Money.formatWhole(s.safeToSpend.perDayMinor)}.")
        appendLine("Week-over-week spend is ${trendWord(s.weekOverWeek.direction, s.weekOverWeek.percentChange)}.")
    }.trim()

    /**
     * Facts for a yes/no affordability question. The verdict is computed
     * deterministically by [AgentService] and passed in — the AI only phrases it.
     */
    fun affordabilityFacts(amountMinor: Long, canAfford: Boolean, poolMinor: Long): String = buildString {
        appendLine("You asked about spending ${Money.formatWhole(amountMinor)}.")
        appendLine("Discretionary money left this month: ${Money.formatWhole(poolMinor)}.")
        appendLine("Verdict: ${if (canAfford) "YES, it fits" else "NO, it's over"}.")
        if (canAfford) {
            appendLine("Money left afterwards: ${Money.formatWhole(poolMinor - amountMinor)}.")
        } else {
            appendLine("Short by: ${Money.formatWhole(amountMinor - poolMinor)}.")
        }
    }.trim()
}
