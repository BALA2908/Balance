package com.balance.budget.domain.recurring

import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.domain.model.Recurring
import com.balance.budget.domain.model.RecurringCadence
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

/** One upcoming bill: the recurring item plus how many days until it's due. */
data class UpcomingBill(
    val recurring: Recurring,
    val daysUntil: Int, // negative = overdue, 0 = today
)

/** Deterministic view of recurring commitments as subscriptions / upcoming bills. */
data class BillsSummary(
    val monthlyTotalMinor: Long,
    val activeCount: Int,
    val upcoming: List<UpcomingBill>, // soonest first
)

/**
 * Pure analyzer: turns active [Recurring] items into a normalized monthly total
 * and a sorted upcoming-bills timeline. Weekly items are normalized to a monthly
 * equivalent (×52/12). All math here, never the AI layer.
 */
object BillSchedule {

    fun monthlyEquivalentMinor(r: Recurring): Long = when (r.cadence) {
        RecurringCadence.WEEKLY -> (r.amountMinor * 52.0 / 12.0).roundToLong()
        RecurringCadence.MONTHLY -> r.amountMinor
    }

    fun compute(recurring: List<Recurring>, nowMillis: Long): BillsSummary {
        val active = recurring.filter { it.isActive }
        val zone = DateTimeUtil.zone
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val upcoming = active
            .map { r ->
                val due = Instant.ofEpochMilli(r.nextDueDate).atZone(zone).toLocalDate()
                UpcomingBill(r, ChronoUnit.DAYS.between(today, due).toInt())
            }
            .sortedBy { it.recurring.nextDueDate }
        return BillsSummary(
            monthlyTotalMinor = active.sumOf { monthlyEquivalentMinor(it) },
            activeCount = active.size,
            upcoming = upcoming,
        )
    }
}
