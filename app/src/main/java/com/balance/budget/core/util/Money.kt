package com.balance.budget.core.util

import java.math.RoundingMode
import kotlin.math.abs

/**
 * All money in the app is stored and computed as [Long] **minor units** (paise).
 * Floating point is never used for currency math. Formatting to ₹ happens only
 * at the UI edge, via the helpers here.
 *
 * 1 rupee = 100 paise. ₹1,234.56 -> 123456L
 *
 * Formatting is **deterministic** and does NOT depend on the device/JVM locale:
 * the Indian lakh/crore grouping is applied by hand. (`java.text.DecimalFormat`
 * only supports a single uniform grouping size, so it can't produce 1,23,456 on
 * the host JVM even though Android's ICU can — we never rely on it.)
 */
object Money {

    const val RUPEE = "₹"

    /** ₹1,23,456.78 — full Indian-grouped string with the rupee symbol. */
    fun format(minor: Long): String = buildString {
        if (minor < 0) append('-')
        val absMinor = abs(minor)
        append(RUPEE)
        append(groupIndian(absMinor / 100))
        append('.')
        append((absMinor % 100).toString().padStart(2, '0'))
    }

    /** ₹1,23,457 — rounded to the nearest whole rupee, used for big hero numbers. */
    fun formatWhole(minor: Long): String = buildString {
        if (minor < 0) append('-')
        val rupees = (abs(minor) + 50) / 100 // round half up to nearest rupee, exact Long math
        append(RUPEE)
        append(groupIndian(rupees))
    }

    /** Just the grouped number, no symbol: "1,23,456.78". */
    fun formatPlain(minor: Long): String = buildString {
        if (minor < 0) append('-')
        val absMinor = abs(minor)
        append(groupIndian(absMinor / 100))
        append('.')
        append((absMinor % 100).toString().padStart(2, '0'))
    }

    /**
     * Indian digit grouping: the first group from the right is 3 digits, then
     * groups of 2 (e.g. 12345678 -> "1,23,45,678"). Input must be non-negative.
     */
    private fun groupIndian(value: Long): String {
        val s = value.toString()
        if (s.length <= 3) return s
        val last3 = s.substring(s.length - 3)
        var rest = s.substring(0, s.length - 3)
        val sb = StringBuilder()
        while (rest.length > 2) {
            sb.insert(0, "," + rest.substring(rest.length - 2))
            rest = rest.substring(0, rest.length - 2)
        }
        if (rest.isNotEmpty()) sb.insert(0, rest)
        return "$sb,$last3"
    }

    /**
     * Parse a user-typed rupee string ("1234.5", "1,234.50") into paise.
     * Returns null when the string isn't a valid non-negative amount.
     */
    fun parseToMinor(input: String): Long? {
        val cleaned = input.replace(",", "").trim()
        if (cleaned.isEmpty()) return null
        val value = cleaned.toBigDecimalOrNull() ?: return null
        if (value.signum() < 0) return null
        return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    }
}
