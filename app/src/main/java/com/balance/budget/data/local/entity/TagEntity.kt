package com.balance.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A lightweight label that can span categories — e.g. "Goa trip", "Diwali",
 * "Work". An expense can carry several. Tags power the History filter and the
 * cross-category "trip recap".
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "color_hex") val colorHex: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
)
