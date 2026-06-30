package com.balance.budget.domain.analytics

import kotlin.math.roundToInt

/**
 * A deterministic, encouraging read on financial health — discipline, savings,
 * and investment — derived purely from the snapshot (+ optional monthly income).
 * Same inputs → same output. The AI layer may phrase it; it never computes it.
 *
 * Income is optional: when it's unset, savings/recurring-burden read null (hidden
 * in the UI, not zeroed) and the score leans on budget adherence + investing.
 * Recommendations are warm and never shaming.
 */
data class FinancialHealth(
    val disciplineScore: Int,              // 0–100 (floored at 35 so it never reads as failing)
    val investmentSharePercent: Double,    // investment spend / total spend
    val savingsRatePercent: Double?,       // (income − projected month-end) / income, clamped 0..100; null w/o income
    val savedSoFarMinor: Long?,            // income − spent-so-far (may be negative); null w/o income
    val recurringBurdenPercent: Double?,   // active recurring / income; null w/o income
    val budgetAdherencePercent: Double,    // from streaks
    val recommendations: List<String>,
) {
    companion object {
        fun from(s: AnalyticsSnapshot, incomeMinor: Long?): FinancialHealth? {
            val hasIncome = incomeMinor != null && incomeMinor > 0
            if (s.isEmpty && !hasIncome) return null

            val totalSpend = s.monthToDateMinor
            val investmentSpend = s.byCategory.filter { it.iconKey == "investment" }.sumOf { it.spentMinor }
            val investmentShare = if (totalSpend > 0) investmentSpend * 100.0 / totalSpend else 0.0

            val savingsRate: Double?
            val savedSoFar: Long?
            val recurringBurden: Double?
            if (hasIncome) {
                val income = incomeMinor!!
                savingsRate = ((income - s.projection.projectedMonthEndMinor).coerceAtLeast(0L) * 100.0 / income)
                    .coerceIn(0.0, 100.0)
                savedSoFar = income - totalSpend
                recurringBurden = (s.activeRecurringTotalMinor * 100.0 / income).coerceIn(0.0, 100.0)
            } else {
                savingsRate = null; savedSoFar = null; recurringBurden = null
            }

            val adherence = s.streaks.adherencePercent
            val score = disciplineScore(
                hasBudget = s.overallBudgetMinor != null,
                adherence = adherence,
                onTrack = s.projection.onTrack,
                savingsRate = savingsRate,
                investmentShare = investmentShare,
            )
            return FinancialHealth(
                disciplineScore = score,
                investmentSharePercent = investmentShare,
                savingsRatePercent = savingsRate,
                savedSoFarMinor = savedSoFar,
                recurringBurdenPercent = recurringBurden,
                budgetAdherencePercent = adherence,
                recommendations = recommendations(
                    hasIncome = hasIncome,
                    savingsRate = savingsRate,
                    investmentShare = investmentShare,
                    recurringBurden = recurringBurden,
                    adherence = adherence,
                    hasBudget = s.overallBudgetMinor != null,
                ),
            )
        }

        /** Heuristic 0–100, floored at 35 so it's a nudge, never a verdict of failure. */
        internal fun disciplineScore(
            hasBudget: Boolean,
            adherence: Double,
            onTrack: Boolean?,
            savingsRate: Double?,
            investmentShare: Double,
        ): Int {
            var score = 55.0
            if (hasBudget) {
                score += (adherence.coerceIn(0.0, 100.0) / 100.0) * 25.0   // up to +25
                score += when (onTrack) { true -> 10.0; false -> -8.0; null -> 0.0 }
            } else {
                score += 8.0 // no budget to judge against — mild neutral
            }
            if (savingsRate != null) score += (savingsRate.coerceIn(0.0, 25.0) / 25.0) * 12.0 // up to +12
            if (investmentShare >= 5.0) score += 6.0
            return score.roundToInt().coerceIn(35, 100)
        }

        private fun recommendations(
            hasIncome: Boolean,
            savingsRate: Double?,
            investmentShare: Double,
            recurringBurden: Double?,
            adherence: Double,
            hasBudget: Boolean,
        ): List<String> {
            val out = mutableListOf<String>()
            if (hasIncome && savingsRate != null) {
                when {
                    savingsRate >= 20 -> out += "You're on pace to save ~${savingsRate.roundToInt()}% this month — excellent. Automating a transfer on payday makes it effortless."
                    savingsRate < 10 -> out += "Saving ~${savingsRate.roundToInt()}% so far — even nudging toward 15% quietly builds a real cushion."
                }
            }
            when {
                investmentShare < 5 -> out += "Investing is a small slice right now — a tiny recurring SIP toward ~10% compounds more than it feels like."
                investmentShare >= 10 -> out += "Investing ~${investmentShare.roundToInt()}% of your spend — future-you is grateful. 🌱"
            }
            if (recurringBurden != null && recurringBurden >= 35) {
                out += "Subscriptions & bills are ~${recurringBurden.roundToInt()}% of income — a quick audit could free up real room."
            }
            if (hasBudget && adherence >= 80) {
                out += "You've stayed under budget on ~${adherence.roundToInt()}% of days — that's genuine discipline. 🔥"
            }
            if (out.isEmpty()) {
                out += "Spending a little less than you earn, consistently, is the whole game — and you're playing it."
            }
            return out.take(3)
        }
    }
}
