package com.balance.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.balance.budget.domain.model.RecurringCadence

/**
 * A recurring expense template (rent, subscriptions). The deterministic engine
 * uses active recurring items to reserve money for "safe-to-spend". Actual
 * generated expenses are written as normal rows with source = RECURRING
 * (materialization logic lands in Phase 5).
 */
@Entity(
    tableName = "recurring",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [Index("category_id")],
)
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val note: String? = null,
    val cadence: RecurringCadence = RecurringCadence.MONTHLY,
    /** Day-of-month (1..28) for MONTHLY, or day-of-week (1..7) for WEEKLY. */
    @ColumnInfo(name = "anchor_day") val anchorDay: Int,
    /** Next due date as epoch millis; advanced as the item materializes. */
    @ColumnInfo(name = "next_due_date") val nextDueDate: Long,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
)
