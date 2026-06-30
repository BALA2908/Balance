package com.balance.budget.domain.story

import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.core.util.Money
import com.balance.budget.domain.analytics.AnalyticsSnapshot
import com.balance.budget.domain.analytics.TrendDirection
import kotlin.math.abs
import kotlin.math.roundToInt

/** One full-screen card in the weekly/monthly Money Story. */
data class StoryCard(
    val emoji: String,
    val headline: String,
    /** Optional hero amount (paise) to count up; null = no number. */
    val amountMinor: Long?,
    val body: String,
    val accentHex: String,
)

/**
 * Builds the Money Story deck deterministically from the snapshot. The AI (if
 * available) only contributes the optional "in a sentence" narration card — every
 * number/card here is computed by the engine.
 */
object MoneyStoryBuilder {

    private const val AMBER = "#F0A868"
    private const val SAGE = "#8FB996"
    private const val HONEY = "#E8C06B"
    private const val CLAY = "#E08B7B"

    fun build(snapshot: AnalyticsSnapshot, narration: String?): List<StoryCard> {
        val month = DateTimeUtil.monthLabel(snapshot.month)
        if (snapshot.isEmpty) {
            return listOf(
                StoryCard("🌱", "A fresh start", null,
                    "No spending logged yet in $month. Add a few expenses and your story will grow.", AMBER)
            )
        }

        val cards = mutableListOf<StoryCard>()
        cards += StoryCard("📖", "Your money story", null, "A quick look at $month so far.", AMBER)
        cards += StoryCard(
            "💸", "You've spent", snapshot.monthToDateMinor,
            snapshot.overallBudgetMinor?.let { "of your ${Money.formatWhole(it)} budget" } ?: "this month",
            AMBER,
        )

        snapshot.topCategories.firstOrNull()?.let { top ->
            cards += StoryCard(
                "🏆", "Top category", top.spentMinor,
                "${top.name} — ${top.percentOfTotal.roundToInt()}% of your spending",
                top.colorHex,
            )
        }

        val mom = snapshot.monthOverMonth
        if (mom.previousMinor > 0) {
            val word = when (mom.direction) {
                TrendDirection.UP -> "up"
                TrendDirection.DOWN -> "down"
                TrendDirection.FLAT -> "about the same"
            }
            val pct = mom.percentChange?.let { " ${abs(it).roundToInt()}%" } ?: ""
            cards += StoryCard(
                if (mom.direction == TrendDirection.DOWN) "📉" else "📈",
                "Vs last month", null,
                "You're $word$pct compared to this point last month.",
                if (mom.direction == TrendDirection.UP) CLAY else SAGE,
            )
        }

        if (snapshot.overallBudgetMinor != null) {
            val onTrack = snapshot.projection.onTrack == true
            cards += StoryCard(
                "🔮", "At this pace", snapshot.projection.projectedMonthEndMinor,
                if (onTrack) "On track to stay within budget — about ${Money.formatWhole(snapshot.safeToSpend.perDayMinor)}/day safe to spend."
                else "Trending a little over — easing up slightly keeps you on track.",
                if (onTrack) SAGE else HONEY,
            )
        }

        if (snapshot.streaks.currentUnderBudgetDays >= 2) {
            cards += StoryCard(
                "🔥", "On a roll", null,
                "${snapshot.streaks.currentUnderBudgetDays} days under budget. Keep it going!",
                SAGE,
            )
        }

        snapshot.financialHealth?.let { h ->
            val body = h.savingsRatePercent?.let {
                "On pace to save about ${it.roundToInt()}% this month — that's how wealth quietly grows. 🌱"
            } ?: if (h.investmentSharePercent >= 1) {
                "Investing ${h.investmentSharePercent.roundToInt()}% of your spend — small, steady habits compound. 🌱"
            } else {
                "Every mindful choice is a brick in something bigger. You're building it. 🌱"
            }
            cards += StoryCard("🚀", "Building wealth", null, body, SAGE)
        }

        narration?.takeIf { it.isNotBlank() }?.let {
            cards += StoryCard("✨", "In a sentence", null, it, AMBER)
        }

        cards += StoryCard("🌙", "That's your story", null, "Small, mindful choices add up. See you tomorrow.", AMBER)
        return cards
    }
}
