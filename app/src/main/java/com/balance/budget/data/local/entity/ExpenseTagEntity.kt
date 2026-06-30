package com.balance.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction row linking an expense to a tag (many-to-many). Both sides CASCADE so
 * deleting an expense or a tag cleans up its links. The composite PK already
 * indexes (expense_id, …); we add an explicit index on tag_id for its FK.
 */
@Entity(
    tableName = "expense_tags",
    primaryKeys = ["expense_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expense_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tag_id")],
)
data class ExpenseTagEntity(
    @ColumnInfo(name = "expense_id") val expenseId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)
