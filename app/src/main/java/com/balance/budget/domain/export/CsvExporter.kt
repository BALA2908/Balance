package com.balance.budget.domain.export

import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.domain.model.ExpenseWithCategory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Builds a spreadsheet-friendly CSV from expenses. Pure and testable. Amounts are
 * rendered as plain rupee decimals (paise → ₹, no symbol/grouping) so they parse
 * cleanly in any spreadsheet.
 */
object CsvExporter {

    private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)
    private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

    fun buildCsv(rows: List<ExpenseWithCategory>, zone: ZoneId = DateTimeUtil.zone): String {
        val sb = StringBuilder()
        sb.append("Date,Time,Category,Note,Merchant,Source,Amount (INR)\n")
        rows.forEach { row ->
            val dt = Instant.ofEpochMilli(row.expense.timestamp).atZone(zone)
            sb.append(escape(dt.format(DATE))).append(',')
            sb.append(escape(dt.format(TIME))).append(',')
            sb.append(escape(row.category.name)).append(',')
            sb.append(escape(row.expense.note ?: "")).append(',')
            sb.append(escape(row.expense.merchant ?: "")).append(',')
            sb.append(escape(row.expense.source.name)).append(',')
            sb.append(rupees(row.expense.amountMinor)).append('\n')
        }
        return sb.toString()
    }

    private fun rupees(minor: Long): String {
        val sign = if (minor < 0) "-" else ""
        val abs = kotlin.math.abs(minor)
        return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }

    /** RFC-4180-ish escaping: quote fields containing comma, quote, or newline. */
    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
