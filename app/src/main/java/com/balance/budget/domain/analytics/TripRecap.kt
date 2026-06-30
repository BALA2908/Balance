package com.balance.budget.domain.analytics

import com.balance.budget.domain.model.ExpenseWithCategory

/** One category's slice of a tag's spend (for the trip recap). */
data class RecapCategory(
    val categoryId: Long,
    val name: String,
    val colorHex: String,
    val iconKey: String,
    val spentMinor: Long,
    val percentOfTotal: Double,
)

/**
 * A cross-category summary of every expense carrying one tag — the "trip recap".
 * Pure: same rows → same recap. The AI layer may phrase it, never compute it.
 */
data class TripRecap(
    val totalMinor: Long,
    val count: Int,
    val firstMillis: Long?,
    val lastMillis: Long?,
    val byCategory: List<RecapCategory>,
) {
    companion object {
        val EMPTY = TripRecap(0, 0, null, null, emptyList())

        fun from(rows: List<ExpenseWithCategory>): TripRecap {
            if (rows.isEmpty()) return EMPTY
            val total = rows.sumOf { it.expense.amountMinor }
            val byCat = rows.groupBy { it.expense.categoryId }
                .map { (id, rs) ->
                    val c = rs.first().category
                    val spent = rs.sumOf { it.expense.amountMinor }
                    RecapCategory(
                        categoryId = id,
                        name = c.name,
                        colorHex = c.colorHex,
                        iconKey = c.iconKey,
                        spentMinor = spent,
                        percentOfTotal = if (total > 0) spent * 100.0 / total else 0.0,
                    )
                }
                .sortedByDescending { it.spentMinor }
            return TripRecap(
                totalMinor = total,
                count = rows.size,
                firstMillis = rows.minOf { it.expense.timestamp },
                lastMillis = rows.maxOf { it.expense.timestamp },
                byCategory = byCat,
            )
        }
    }
}
