package com.balance.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.balance.budget.domain.model.BudgetPeriod

/**
 * A budget limit. [categoryId] == null means the overall monthly budget;
 * otherwise it's a per-category cap. Budgets are **versioned** by
 * [effectiveFromYearMonth] (e.g. 202606) so editing this month's budget never
 * rewrites what last month's limit actually was — reports stay historically true.
 *
 * The "active" budget for a given month/category is the row with the greatest
 * effectiveFromYearMonth that is <= that month.
 */
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["category_id", "effective_from_ym"], unique = false)],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** null = overall budget; otherwise a category id. */
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    @ColumnInfo(name = "effective_from_ym") val effectiveFromYearMonth: Int,
)
