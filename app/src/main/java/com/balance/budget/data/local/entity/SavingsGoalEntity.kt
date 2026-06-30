package com.balance.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A manual savings goal — e.g. "Emergency fund", "New phone". The user adds to
 * [savedMinor] as they set money aside; progress = saved / target. Standalone
 * (not derived from expenses) and never shaming.
 */
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "icon_key") val iconKey: String,
    @ColumnInfo(name = "color_hex") val colorHex: String,
    @ColumnInfo(name = "target_minor") val targetMinor: Long,
    @ColumnInfo(name = "saved_minor") val savedMinor: Long = 0,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
)
