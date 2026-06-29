package com.balance.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single "move budget between envelopes" entry for one month — the graceful
 * overspend path ("roll with the punches"): when Travel runs hot, move ₹X from
 * Food into Travel instead of being shamed for it. The analytics engine reads
 * these per month and shifts each category's *effective* budget by the net of
 * what moved in vs out. Reversing a move = inserting the inverse row, so the
 * ledger is append-only and fully auditable.
 *
 * A null [fromCategoryId]/[toCategoryId] means the unallocated/overall pool.
 * No foreign keys: adjustments are historical facts that must survive a category
 * being archived; the engine simply ignores moves whose category no longer applies.
 */
@Entity(
    tableName = "budget_adjustments",
    indices = [Index(value = ["ym"], unique = false)],
)
data class BudgetAdjustmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Year-month the move applies to, encoded YYYYMM (e.g. 202606). */
    val ym: Int,
    @ColumnInfo(name = "from_category_id") val fromCategoryId: Long? = null,
    @ColumnInfo(name = "to_category_id") val toCategoryId: Long? = null,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
