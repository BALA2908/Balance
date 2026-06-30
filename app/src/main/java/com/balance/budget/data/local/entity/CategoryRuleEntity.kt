package com.balance.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-defined auto-categorization rule: if an expense's merchant/note contains
 * [pattern] (case-insensitive), assign [categoryId]. Rules sit ABOVE the learned
 * categorizer — an explicit rule always wins. Lower [sortOrder] = higher priority;
 * the first matching rule applies.
 */
@Entity(
    tableName = "category_rules",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("category_id")],
)
data class CategoryRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Case-insensitive substring matched against merchant (then note). */
    val pattern: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
)
