package com.balance.budget.domain.receipt

import com.balance.budget.core.util.Money

/**
 * Pure parser: turns raw OCR text from a receipt into a best-guess amount and
 * merchant. Deterministic and unit-testable — the OCR (Android/ML Kit) just
 * supplies the text. The user always confirms in Quick Add, so a good guess is
 * enough; we never auto-save.
 */
object ReceiptParser {

    data class Parsed(val amountMinor: Long?, val merchant: String?)

    // Money-looking tokens: prefer decimals (12,345.67) but also accept plain runs.
    private val decimalNumber = Regex("""\d[\d,]*\.\d{1,2}""")
    private val anyNumber = Regex("""\d[\d,]*(?:\.\d{1,2})?""")

    fun parse(rawText: String): Parsed {
        val lines = rawText.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        return Parsed(amountMinor = extractAmount(lines), merchant = extractMerchant(lines))
    }

    private fun extractMerchant(lines: List<String>): String? =
        lines.firstOrNull { line ->
            line.count { it.isLetter() } >= 3 &&
                !line.contains("receipt", ignoreCase = true) &&
                !line.contains("invoice", ignoreCase = true) &&
                !line.contains("gst", ignoreCase = true)
        }?.take(40)

    private fun extractAmount(lines: List<String>): Long? {
        fun moneyIn(s: String, regex: Regex): List<Long> =
            regex.findAll(s).map { it.value.replace(",", "") }.mapNotNull { Money.parseToMinor(it) }.toList()

        // 1. A "total"/"amount" line (but not subtotal) — receipts put the real total here.
        val totalLine = lines.lastOrNull { raw ->
            val l = raw.lowercase()
            ("total" in l || "amount" in l || "amt" in l || "grand" in l) &&
                "subtotal" !in l && "sub total" !in l
        }
        totalLine?.let { line ->
            (moneyIn(line, decimalNumber).maxOrNull() ?: moneyIn(line, anyNumber).maxOrNull())?.let { return it }
        }

        // 2. Otherwise the largest decimal amount anywhere (most receipts show ₹x.xx).
        lines.flatMap { moneyIn(it, decimalNumber) }.maxOrNull()?.let { return it }

        // 3. Last resort: the largest plain number.
        return lines.flatMap { moneyIn(it, anyNumber) }.maxOrNull()
    }
}
