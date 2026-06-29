package com.balance.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.balance.budget.domain.model.ExpenseSource

/**
 * A single expense. Amount is stored in **paise** (minor units) as a Long —
 * never a floating-point rupee value. [timestamp] is when the spend happened
 * (editable, for backfill); [createdAt] is when the row was written.
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [Index("category_id"), Index("timestamp")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val note: String? = null,
    /** When the expense occurred (epoch millis, UTC). */
    val timestamp: Long,
    /** When the row was created (epoch millis, UTC). */
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val source: ExpenseSource = ExpenseSource.MANUAL,
    /** Parsed merchant name, populated by auto-import (Phase 4). */
    val merchant: String? = null,
)
