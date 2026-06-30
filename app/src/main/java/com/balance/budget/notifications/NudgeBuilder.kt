package com.balance.budget.notifications

import com.balance.budget.core.util.Money
import com.balance.budget.domain.analytics.AnalyticsSnapshot
import com.balance.budget.domain.analytics.SafeToSpendBasis

data class Nudge(val title: String, val text: String)

/**
 * Pure: turns a deterministic [AnalyticsSnapshot] into at most one calm, non-shaming
 * nudge — or null when there's nothing worth interrupting for. Priority: over-budget
 * → unusual expense → off-track projection → quiet (no spam). The engine computed
 * every number; this only phrases it.
 */
object NudgeBuilder {
    fun build(s: AnalyticsSnapshot): Nudge? {
        if (s.isEmpty) return null

        if (s.safeToSpend.basis == SafeToSpendBasis.EXHAUSTED) {
            return Nudge(
                title = "Over budget this month",
                text = "You've passed this month's budget — take it easy for the last stretch 🌙",
            )
        }

        s.anomalies.firstOrNull()?.let { a ->
            return Nudge(
                title = "A bigger ${a.categoryName} expense",
                text = "${Money.format(a.amountMinor)} on ${a.categoryName} is larger than usual — worth a glance.",
            )
        }

        val projection = s.projection
        if (projection.onTrack == false) {
            val over = projection.projectedOverBudgetMinor ?: 0L
            if (over > 0L) {
                return Nudge(
                    title = "On pace to go over",
                    text = "At this rate you'll land ~${Money.format(over)} over budget by month-end. A small slow-down keeps you green.",
                )
            }
        }
        return null
    }
}
