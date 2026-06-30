package com.balance.budget.domain.analytics

import kotlin.math.roundToInt

/**
 * Picks at most one warm, motivating one-liner for a screen, **deterministically
 * and day-seeded** — so it's stable within a day and rotates gently across days
 * (never random, never flickering on recomposition). Built straight from the
 * snapshot; the engine's numbers, lightly phrased. Returns null when there's
 * nothing worth saying (empty month) so the UI stays calm.
 */
object MicrocopySelector {

    /** @param dayKey a stable per-day integer (e.g. LocalDate.toEpochDay()). */
    fun lineFor(s: AnalyticsSnapshot, dayKey: Int): String? {
        if (s.isEmpty) return null
        val candidates = buildList {
            if (s.streaks.currentUnderBudgetDays >= 3) {
                add("🔥 ${s.streaks.currentUnderBudgetDays} days under budget — lovely momentum.")
            }
            s.financialHealth?.savingsRatePercent?.let { rate ->
                if (rate >= 15) add("🌱 On pace to save about ${rate.roundToInt()}% this month.")
            }
            s.financialHealth?.let { h ->
                if (h.investmentSharePercent >= 8) add("📈 Investing ${h.investmentSharePercent.roundToInt()}% of your spend — future-you approves.")
            }
            if (s.projection.onTrack == true) add("You're on track to finish under budget — nicely done. ✨")
            if (s.rolloverCarryMinor > 0) add("🌱 Last month's leftover is working for you this month.")
            // Always-present gentle evergreens so there's a calm line to fall back on.
            add("Small, mindful choices add up. 🌙")
            add("Every expense you log is a moment of clarity. 🌿")
        }
        return candidates[dayKey.mod(candidates.size)]
    }
}
