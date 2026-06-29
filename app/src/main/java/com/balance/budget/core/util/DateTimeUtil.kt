package com.balance.budget.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Time helpers. Timestamps are stored as epoch millis (UTC); all display and
 * "which month / which week" logic resolves against the device's local zone.
 * minSdk 34 means java.time is available natively — no desugaring needed.
 */
object DateTimeUtil {

    val zone: ZoneId get() = ZoneId.systemDefault()

    fun localDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun yearMonth(epochMillis: Long): YearMonth = YearMonth.from(localDate(epochMillis))

    /** Compact int key for a month, e.g. June 2026 -> 202606. Used by budgets. */
    fun yearMonthKey(ym: YearMonth): Int = ym.year * 100 + ym.monthValue

    fun startOfMonthMillis(ym: YearMonth): Long =
        ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfMonthMillis(ym: YearMonth): Long =
        ym.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    /** "Today", "Yesterday", or "12 Jun". */
    fun friendlyDate(epochMillis: Long, today: LocalDate = LocalDate.now(zone)): String {
        val d = localDate(epochMillis)
        return when (d) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> if (d.year == today.year) d.format(dayMonth) else d.format(dayMonthYear)
        }
    }

    fun timeOfDay(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(timeFmt)

    fun monthLabel(ym: YearMonth): String =
        ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
}
