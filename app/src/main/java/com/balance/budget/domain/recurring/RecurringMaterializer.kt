package com.balance.budget.domain.recurring

import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.domain.model.Recurring
import com.balance.budget.domain.model.RecurringCadence
import java.time.Instant
import java.time.ZoneId
import kotlin.math.min

/**
 * Pure logic that turns due recurring items into real expenses. Idempotent by
 * construction: it generates an expense for every due date at or before "now"
 * and advances [Recurring.nextDueDate] past now, so re-running never double-charges
 * (the advanced date is the source of truth). Catches up across multiple missed
 * periods (e.g. app not opened for two months → two rent expenses).
 */
object RecurringMaterializer {

    /** A safety bound on catch-up generation (well above any real backlog). */
    private const val MAX_CATCHUP = 366

    data class MaterializedExpense(
        val amountMinor: Long,
        val categoryId: Long,
        val note: String?,
        val timestamp: Long,
    )

    data class Result(
        val expenses: List<MaterializedExpense>,
        val newNextDueDate: Long,
    )

    fun materialize(
        recurring: Recurring,
        nowMillis: Long,
        zone: ZoneId = DateTimeUtil.zone,
    ): Result {
        if (!recurring.isActive) return Result(emptyList(), recurring.nextDueDate)

        var due = recurring.nextDueDate
        val out = mutableListOf<MaterializedExpense>()
        var guard = 0
        while (due <= nowMillis && guard < MAX_CATCHUP) {
            out += MaterializedExpense(
                amountMinor = recurring.amountMinor,
                categoryId = recurring.categoryId,
                note = recurring.note,
                timestamp = due,
            )
            due = nextOccurrence(recurring.cadence, recurring.anchorDay, due, zone)
            guard++
        }
        return Result(out, due)
    }

    private fun nextOccurrence(
        cadence: RecurringCadence,
        anchorDay: Int,
        fromMillis: Long,
        zone: ZoneId,
    ): Long {
        val date = Instant.ofEpochMilli(fromMillis).atZone(zone).toLocalDate()
        val next = when (cadence) {
            RecurringCadence.MONTHLY -> {
                val month = date.plusMonths(1)
                month.withDayOfMonth(min(anchorDay.coerceIn(1, 28), month.lengthOfMonth()))
            }
            RecurringCadence.WEEKLY -> date.plusWeeks(1)
        }
        return next.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
