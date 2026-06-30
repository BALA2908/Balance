package com.balance.budget.domain.receipt

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptParserTest {

    @Test fun `picks the total line over subtotal`() {
        val text = """
            Third Wave Coffee
            Cappuccino      180.00
            Croissant       140.00
            Subtotal        320.00
            GST              16.00
            Total           336.00
            Thank you!
        """.trimIndent()
        val p = ReceiptParser.parse(text)
        assertEquals(336_00L, p.amountMinor)
        assertEquals("Third Wave Coffee", p.merchant)
    }

    @Test fun `falls back to the largest decimal amount`() {
        val text = """
            BigBazaar
            Item A 50.00
            Item B 1,250.50
            Item C 99.00
        """.trimIndent()
        val p = ReceiptParser.parse(text)
        assertEquals(1_250_50L, p.amountMinor)
        assertEquals("BigBazaar", p.merchant)
    }

    @Test fun `handles comma grouping on the total line`() {
        val text = "Store\nGrand Total  ₹ 12,345.67"
        assertEquals(12_345_67L, ReceiptParser.parse(text).amountMinor)
    }

    @Test fun `blank text yields nulls`() {
        val p = ReceiptParser.parse("   \n  ")
        assertEquals(null, p.amountMinor)
        assertEquals(null, p.merchant)
    }
}
