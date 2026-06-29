package com.balance.budget.data.categorize

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategorizerTest {

    @Test fun `encode then decode round-trips`() {
        val counts = mapOf(1L to 3, 2L to 1, 5L to 9)
        assertEquals(counts, CategorizerStore.decode(CategorizerStore.encode(counts)))
    }

    @Test fun `decode tolerates junk and empty`() {
        assertEquals(emptyMap<Long, Int>(), CategorizerStore.decode(""))
        assertEquals(mapOf(1L to 2), CategorizerStore.decode("1:2,garbage,x:y,3:"))
    }

    @Test fun `normalize lowercases and collapses whitespace`() {
        assertEquals("third wave coffee", Categorizer.normalize("  Third   Wave  Coffee "))
        assertEquals("swiggy", Categorizer.normalize("SWIGGY"))
    }

    @Test fun `normalize returns null for blank`() {
        assertNull(Categorizer.normalize(null))
        assertNull(Categorizer.normalize("   "))
    }
}
