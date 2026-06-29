package com.balance.budget.service.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests the UPI parser against representative notification strings for the three
 * supported apps, plus the cases we must NOT import (credits, requests, summaries,
 * unknown apps).
 */
class UpiNotificationParserTest {

    private val gpay = UpiApp.GPAY.packageName
    private val phonepe = UpiApp.PHONEPE.packageName
    private val paytm = UpiApp.PAYTM.packageName
    private val now = 1_700_000_000_000L

    @Test fun `gpay paid-to is parsed as debit`() {
        val r = UpiNotificationParser.parse(gpay, "Google Pay", "You paid ₹149 to Third Wave Coffee via UPI", now)!!
        assertEquals(149_00L, r.amountMinor)
        assertEquals("Third Wave Coffee", r.merchant)
        assertEquals(UpiApp.GPAY, r.app)
    }

    @Test fun `phonepe paid with grouping separator`() {
        val r = UpiNotificationParser.parse(phonepe, "PhonePe", "Paid ₹1,250 to Reliance Fresh.", now)!!
        assertEquals(1_250_00L, r.amountMinor)
        assertEquals("Reliance Fresh", r.merchant)
    }

    @Test fun `paytm sent-to is a debit`() {
        val r = UpiNotificationParser.parse(paytm, "Paytm", "₹80 sent to Auto Driver", now)!!
        assertEquals(80_00L, r.amountMinor)
        assertEquals("Auto Driver", r.merchant)
    }

    @Test fun `debit without a clear merchant still parses the amount`() {
        val r = UpiNotificationParser.parse(gpay, "Google Pay", "₹500 debited for UPI payment", now)!!
        assertEquals(500_00L, r.amountMinor)
    }

    @Test fun `credit is not imported`() {
        assertNull(UpiNotificationParser.parse(gpay, "Google Pay", "You received ₹500 from Mom", now))
    }

    @Test fun `payment request is not imported`() {
        assertNull(UpiNotificationParser.parse(phonepe, "PhonePe", "Rahul is requesting ₹200", now))
    }

    @Test fun `grouped summary with no amount yields null`() {
        assertNull(UpiNotificationParser.parse(gpay, "Google Pay", "You have 3 new notifications", now))
    }

    @Test fun `unknown app yields null`() {
        assertNull(UpiNotificationParser.parse("com.whatsapp", "WhatsApp", "You paid ₹100 to Sam", now))
    }
}
