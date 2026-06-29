package com.balance.budget.domain.recurring

import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.domain.model.Recurring
import com.balance.budget.domain.model.RecurringCadence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurringMaterializerTest {

    private val zone = DateTimeUtil.zone
    private fun dayStart(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(zone).toInstant().toEpochMilli()

    private val now = dayStart(2026, 6, 29) + 12 * 60 * 60 * 1000 // 2026-06-29 noon

    private fun monthly(nextDue: Long, day: Int, active: Boolean = true) = Recurring(
        id = 1, amountMinor = 10_000_00, categoryId = 1, note = "Rent",
        cadence = RecurringCadence.MONTHLY, anchorDay = day, nextDueDate = nextDue, isActive = active,
    )

    @Test fun `monthly catches up missed periods without double-charging`() {
        val result = RecurringMaterializer.materialize(monthly(dayStart(2026, 5, 5), 5), now)
        assertEquals(2, result.expenses.size) // May 5 and June 5
        assertEquals(dayStart(2026, 5, 5), result.expenses[0].timestamp)
        assertEquals(dayStart(2026, 6, 5), result.expenses[1].timestamp)
        assertEquals(dayStart(2026, 7, 5), result.newNextDueDate) // advanced past now

        // Re-running with the advanced date generates nothing more (idempotent).
        val second = RecurringMaterializer.materialize(monthly(result.newNextDueDate, 5), now)
        assertTrue(second.expenses.isEmpty())
        assertEquals(dayStart(2026, 7, 5), second.newNextDueDate)
    }

    @Test fun `not-yet-due generates nothing`() {
        val result = RecurringMaterializer.materialize(monthly(dayStart(2026, 7, 10), 10), now)
        assertTrue(result.expenses.isEmpty())
        assertEquals(dayStart(2026, 7, 10), result.newNextDueDate)
    }

    @Test fun `inactive generates nothing`() {
        val result = RecurringMaterializer.materialize(monthly(dayStart(2026, 5, 5), 5, active = false), now)
        assertTrue(result.expenses.isEmpty())
    }

    @Test fun `weekly catches up each week up to now`() {
        val weekly = Recurring(
            id = 2, amountMinor = 500_00, categoryId = 1, note = "Subscription",
            cadence = RecurringCadence.WEEKLY, anchorDay = 1, nextDueDate = dayStart(2026, 6, 15), isActive = true,
        )
        val result = RecurringMaterializer.materialize(weekly, now)
        assertEquals(3, result.expenses.size) // June 15, 22, 29
        assertEquals(dayStart(2026, 7, 6), result.newNextDueDate)
    }
}
