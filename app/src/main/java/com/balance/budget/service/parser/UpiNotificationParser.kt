package com.balance.budget.service.parser

import com.balance.budget.core.util.Money

/**
 * Heuristically extracts a **debit** (money you paid out → an expense) from a UPI
 * payment notification. Pure and deterministic so it's fully unit-testable.
 *
 * Deliberately conservative: it only returns a result when the text clearly looks
 * like a payment sent (not a credit/refund/request) AND an amount is present.
 * Summary/grouped notifications (no amount) naturally yield null, so the listener
 * can hand us every notification and we ignore the noise.
 *
 * NOTE: real notification wording varies by app version and changes over time.
 * These patterns are sensible defaults — verify/tune against real notifications
 * on the device (see UpiNotificationParserTest for the captured samples).
 */
object UpiNotificationParser {

    private val DEBIT_HINTS = listOf("paid", "sent", "debited", "spent", "paying", "payment of")
    private val CREDIT_HINTS = listOf("received", "credited", "added to", "refund", "requesting", "requested", "request for")

    // ₹1,234.56 / Rs. 1234 / INR 80
    private val AMOUNT = Regex(
        """(?:₹|rs\.?|inr)\s?([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )

    // "...to <Merchant> via UPI" / "...to <Merchant>." — capture the payee.
    private val MERCHANT = Regex(
        """\bto\s+([^.,!?\n]+?)(?:\s+(?:via|using|on|for|is|was|through)\b|[.,!?\n]|$)""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(packageName: String, title: String?, text: String?, postedAt: Long): ParsedPayment? {
        val app = UpiApp.fromPackage(packageName) ?: return null
        val combined = listOfNotNull(title, text).joinToString(" ").trim()
        if (combined.isBlank()) return null

        val lower = combined.lowercase()
        val looksDebit = DEBIT_HINTS.any { lower.contains(it) }
        val looksCredit = CREDIT_HINTS.any { lower.contains(it) }
        if (!looksDebit || looksCredit) return null

        val amountStr = AMOUNT.find(combined)?.groupValues?.getOrNull(1) ?: return null
        val amountMinor = Money.parseToMinor(amountStr) ?: return null
        if (amountMinor <= 0L) return null

        val merchant = MERCHANT.find(combined)?.groupValues?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.length in 2..40 }

        return ParsedPayment(
            amountMinor = amountMinor,
            merchant = merchant,
            app = app,
            rawText = combined,
            postedAt = postedAt,
        )
    }
}
