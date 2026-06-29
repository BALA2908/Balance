package com.balance.budget.data.repository

import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.data.local.dao.RecurringDao
import com.balance.budget.data.local.entity.RecurringEntity
import com.balance.budget.domain.model.Recurring
import com.balance.budget.domain.model.RecurringCadence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import kotlin.math.min
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepository @Inject constructor(
    private val recurringDao: RecurringDao,
    private val clock: () -> Long,
) {
    fun observeAll(): Flow<List<Recurring>> =
        recurringDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(): Flow<List<Recurring>> =
        recurringDao.observeActive().map { list -> list.map { it.toDomain() } }

    /** Total active monthly commitments (paise) — feeds the safe-to-spend math. */
    fun observeActiveTotal(): Flow<Long> = recurringDao.observeActiveTotal()

    /** One-shot snapshot of active recurring items (for materialization). */
    suspend fun activeOnce(): List<Recurring> = recurringDao.getActive().map { it.toDomain() }

    /** Advances only the next-due-date (materialization owns the new value). */
    suspend fun updateNextDueDate(id: Long, nextDueDate: Long) =
        recurringDao.updateNextDueDate(id, nextDueDate)

    suspend fun add(
        amountMinor: Long,
        categoryId: Long,
        note: String?,
        cadence: RecurringCadence,
        anchorDay: Int,
    ): Long {
        val entity = RecurringEntity(
            amountMinor = amountMinor,
            categoryId = categoryId,
            note = note?.trim()?.ifEmpty { null },
            cadence = cadence,
            anchorDay = anchorDay,
            nextDueDate = computeNextDueDate(cadence, anchorDay, clock()),
            isActive = true,
        )
        return recurringDao.insert(entity)
    }

    suspend fun update(recurring: Recurring) {
        // Recompute the next due date in case cadence/day changed.
        recurringDao.update(
            recurring.toEntity().copy(
                nextDueDate = computeNextDueDate(recurring.cadence, recurring.anchorDay, clock()),
            )
        )
    }

    suspend fun setActive(recurring: Recurring, active: Boolean) {
        recurringDao.update(recurring.toEntity().copy(isActive = active))
    }

    suspend fun delete(recurring: Recurring) {
        recurringDao.delete(recurring.toEntity())
    }

    /**
     * Next occurrence of a recurring item, used at creation (materialization into
     * real expenses lands in Phase 5). MONTHLY anchorDay is clamped to 1..28 so it
     * exists in every month; WEEKLY anchorDay is a DayOfWeek value (1=Mon..7=Sun).
     */
    private fun computeNextDueDate(cadence: RecurringCadence, anchorDay: Int, nowMillis: Long): Long {
        val zone = DateTimeUtil.zone
        val today: LocalDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val date: LocalDate = when (cadence) {
            RecurringCadence.MONTHLY -> {
                val day = anchorDay.coerceIn(1, 28)
                val thisMonth = today.withDayOfMonth(min(day, today.lengthOfMonth()))
                if (!thisMonth.isBefore(today)) {
                    thisMonth
                } else {
                    val next = today.plusMonths(1)
                    next.withDayOfMonth(min(day, next.lengthOfMonth()))
                }
            }
            RecurringCadence.WEEKLY -> {
                val target = anchorDay.coerceIn(1, 7)
                var dt = today
                while (dt.dayOfWeek.value != target) dt = dt.plusDays(1)
                dt
            }
        }
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
