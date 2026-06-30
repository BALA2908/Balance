package com.balance.budget.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class MicrocopySelectorTest {

    private val month = YearMonth.of(2026, 6)

    @Test fun `empty month yields no line (stays calm)`() {
        val s = AnalyticsSnapshot.empty(month) // isEmpty = true
        assertNull(MicrocopySelector.lineFor(s, dayKey = 123))
    }

    @Test fun `non-empty month always offers a gentle line`() {
        val s = AnalyticsSnapshot.empty(month).copy(isEmpty = false, monthToDateMinor = 1_000_00)
        assertNotNull(MicrocopySelector.lineFor(s, dayKey = 0))
    }

    @Test fun `same day + same snapshot is stable (day-seeded, not random)`() {
        val s = AnalyticsSnapshot.empty(month).copy(isEmpty = false, monthToDateMinor = 1_000_00)
        assertEquals(MicrocopySelector.lineFor(s, 42), MicrocopySelector.lineFor(s, 42))
    }
}
