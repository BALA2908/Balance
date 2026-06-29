package com.balance.budget.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Money is the foundation of every number in the app, so its parsing/rounding
 * is tested hard. All amounts are paise (minor units) — never floats.
 */
class MoneyTest {

    @Test fun `parses whole rupees`() {
        assertEquals(150_00L, Money.parseToMinor("150"))
    }

    @Test fun `parses rupees and paise`() {
        assertEquals(149_50L, Money.parseToMinor("149.50"))
        assertEquals(149_05L, Money.parseToMinor("149.05"))
        assertEquals(149_50L, Money.parseToMinor("149.5"))
    }

    @Test fun `strips grouping separators`() {
        assertEquals(123_456_78L, Money.parseToMinor("1,23,456.78"))
    }

    @Test fun `rounds half up at paise precision`() {
        // 10.005 -> 10.01
        assertEquals(10_01L, Money.parseToMinor("10.005"))
    }

    @Test fun `rejects empty and invalid input`() {
        assertNull(Money.parseToMinor(""))
        assertNull(Money.parseToMinor("   "))
        assertNull(Money.parseToMinor("abc"))
        assertNull(Money.parseToMinor("-50"))
    }

    @Test fun `formats with rupee symbol and indian grouping`() {
        val formatted = Money.format(123_456_78L)
        // Symbol + Indian digit grouping (1,23,456.78)
        assert(formatted.contains("1,23,456.78")) { "was: $formatted" }
        assert(formatted.contains("₹")) { "was: $formatted" }
    }

    @Test fun `formatWhole rounds to nearest rupee`() {
        val formatted = Money.formatWhole(149_60L)
        assert(formatted.contains("150")) { "was: $formatted" }
    }
}
