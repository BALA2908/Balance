package com.balance.budget.domain.ai

import com.balance.budget.core.util.Money
import com.balance.budget.domain.analytics.AnalyticsSnapshot
import com.balance.budget.domain.analytics.SafeToSpendBasis
import kotlin.math.roundToInt

/**
 * Deterministic, warm, non-judgmental natural language built straight from the
 * engine's numbers. This is the always-available path: when no AI model is on
 * hand, these templates render instead — so summaries, tips, forecasts, and
 * affordability answers work fully offline. Tone: kind companion, never shame.
 */
object FallbackCopy {

    private fun pct(p: Double) = p.roundToInt()

    fun analyst(s: AnalyticsSnapshot): String {
        if (s.isEmpty) {
            return "Nothing logged yet this month — a calm start. Add a few expenses and your story will fill in. 🌱"
        }
        return buildString {
            append("You've spent ${Money.formatWhole(s.monthToDateMinor)}")
            val budget = s.overallBudgetMinor
            if (budget != null) {
                val remaining = s.overallRemainingMinor ?: (budget - s.monthToDateMinor)
                if (remaining >= 0) append(" of your ${Money.formatWhole(budget)} budget — ${Money.formatWhole(remaining)} still to go")
                else append(", a little past your ${Money.formatWhole(budget)} budget")
            }
            append(". ")
            s.topCategories.firstOrNull()?.let {
                append("Most went to ${it.name} (${pct(it.percentOfTotal)}%). ")
            }
            val proj = s.projection
            when {
                proj.onTrack == true -> append("At this pace you'll finish around ${Money.formatWhole(proj.projectedMonthEndMinor)} — nicely on track.")
                (proj.projectedOverBudgetMinor ?: 0) > 0 -> append("At this pace you're heading toward ${Money.formatWhole(proj.projectedMonthEndMinor)}, about ${Money.formatWhole(proj.projectedOverBudgetMinor!!)} over — worth a gentle look.")
                else -> append("At this pace you'll finish around ${Money.formatWhole(proj.projectedMonthEndMinor)}.")
            }
        }
    }

    fun advisorTips(s: AnalyticsSnapshot): List<String> {
        val tips = mutableListOf<String>()
        s.byCategory.firstOrNull { it.isOverBudget }?.let {
            tips += "You're a touch over in ${it.name}. Easing off there for a few days will help the most."
        }
        if (s.streaks.currentUnderBudgetDays >= 2) {
            tips += "Nice — ${s.streaks.currentUnderBudgetDays} days under budget. Keep the gentle streak going!"
        }
        if (s.safeToSpend.basis == SafeToSpendBasis.FULL && s.safeToSpend.perDayMinor > 0) {
            tips += "You've got about ${Money.formatWhole(s.safeToSpend.perDayMinor)} a day to enjoy guilt-free."
        }
        val over = s.projection.projectedOverBudgetMinor ?: 0
        if (over > 0 && s.projection.daysRemaining > 0) {
            val perDay = (over.toDouble() / s.projection.daysRemaining).roundToLong()
            tips += "Trimming about ${Money.formatWhole(perDay)} a day brings month-end back on track."
        }
        // Evergreen top-ups so we always offer three.
        val evergreen = listOf(
            "A small daily trim adds up — ₹100 a day is about ₹3,000 a month.",
            "Glancing at your top category once a week keeps surprises away.",
            "Setting a limit on your biggest category makes spending feel calmer.",
        )
        var i = 0
        while (tips.size < 3 && i < evergreen.size) {
            if (evergreen[i] !in tips) tips += evergreen[i]
            i++
        }
        return tips.take(3)
    }

    fun forecast(s: AnalyticsSnapshot): String = buildString {
        append("If spending keeps to this pace, you'll reach about ${Money.formatWhole(s.projection.projectedMonthEndMinor)} by month-end")
        val budget = s.overallBudgetMinor
        if (budget != null) {
            append(if (s.projection.onTrack == true) ", comfortably within your ${Money.formatWhole(budget)} budget" else ", a bit over your ${Money.formatWhole(budget)} budget")
        }
        append(". ")
        if (s.safeToSpend.basis == SafeToSpendBasis.FULL) {
            append("That leaves about ${Money.formatWhole(s.safeToSpend.perDayMinor)} a day to play with.")
        }
    }

    fun financialHealth(s: AnalyticsSnapshot): String {
        val h = s.financialHealth
            ?: return "Add your monthly income in Settings and a few expenses — your financial health will appear here. 🌱"
        val word = when {
            h.disciplineScore >= 80 -> "excellent"
            h.disciplineScore >= 65 -> "solid"
            h.disciplineScore >= 50 -> "steady"
            else -> "finding its feet"
        }
        return buildString {
            append("Your financial health is $word — ${h.disciplineScore}/100. ")
            h.savingsRatePercent?.let { append("You're on pace to save about ${pct(it)}% of your income. ") }
            if (h.investmentSharePercent >= 1) append("Investing is ${pct(h.investmentSharePercent)}% of your spending — every bit compounds.")
        }.trim()
    }

    fun affordability(amountMinor: Long, canAfford: Boolean, poolMinor: Long): String =
        if (canAfford) {
            "Yes — ${Money.formatWhole(amountMinor)} fits. You'd still have about ${Money.formatWhole(poolMinor - amountMinor)} flexible this month."
        } else {
            val short = amountMinor - poolMinor
            "That's a stretch right now — you have about ${Money.formatWhole(poolMinor)} left, so ${Money.formatWhole(amountMinor)} goes ${Money.formatWhole(short)} over. Maybe give it a few days?"
        }

    private fun Double.roundToLong(): Long = Math.round(this)
}
