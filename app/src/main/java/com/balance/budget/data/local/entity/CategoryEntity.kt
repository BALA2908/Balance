package com.balance.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A spending category. Default categories are seeded on first launch; the user
 * can add, rename, recolor, reorder, or archive any of them. We archive rather
 * than hard-delete so historical expenses keep a valid category reference.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Stable key into the icon map in the UI layer (e.g. "food", "travel"). */
    @ColumnInfo(name = "icon_key") val iconKey: String,
    /** Index into CozyColors.categorySwatches, or a stored hex if customized. */
    @ColumnInfo(name = "color_hex") val colorHex: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
)
