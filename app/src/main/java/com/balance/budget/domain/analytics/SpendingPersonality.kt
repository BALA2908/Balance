package com.balance.budget.domain.analytics

import kotlin.math.roundToInt

/** A warm, non-shaming spending archetype for the month. */
data class Personality(val title: String, val emoji: String, val blurb: String)

/**
 * Pure: derives a gentle "spending personality" from the deterministic snapshot —
 * based on which category leads and how concentrated the month is. Never shaming;
 * the AI layer may re-phrase, never compute. Returns null for an empty month.
 */
object SpendingPersonality {

    fun from(s: AnalyticsSnapshot): Personality? {
        if (s.isEmpty || s.monthToDateMinor <= 0) return null
        val top = s.topCategories.firstOrNull() ?: return null
        val share = top.percentOfTotal
        val pct = share.roundToInt()
        return when {
            top.iconKey == "food" && share >= 30 ->
                Personality("The Foodie", "🍜", "Food leads your month at $pct% — delicious, and easy to trim a little if you want to.")
            top.iconKey in setOf("travel", "petrol", "flight", "transit") && share >= 25 ->
                Personality("The Explorer", "🧭", "You're on the move — travel and fuel top your spend this month.")
            top.iconKey in setOf("shopping", "entertainment", "music", "games") && share >= 25 ->
                Personality("Treat-Yourself", "🛍️", "Shopping and fun lead the way — you know how to enjoy your money.")
            top.iconKey in setOf("bills", "health") && share >= 30 ->
                Personality("The Nest-Builder", "🏡", "Mostly essentials and bills — steady, grounded, dependable.")
            top.iconKey in setOf("investment", "savings") && share >= 25 ->
                Personality("The Builder", "📈", "A solid slice goes to investing and saving — future-you says thanks.")
            share < 28 ->
                Personality("The Balanced", "⚖️", "No single category dominates — a nicely balanced month.")
            else ->
                Personality("${top.name} Lover", "✨", "${top.name} is your top category at $pct% this month.")
        }
    }
}
