package com.balance.budget.data.local.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.balance.budget.data.local.entity.CategoryEntity
import com.balance.budget.data.local.entity.ExpenseEntity

/** Room join result: an expense together with its category. */
data class ExpenseWithCategoryRow(
    @Embedded val expense: ExpenseEntity,
    @Relation(parentColumn = "category_id", entityColumn = "id")
    val category: CategoryEntity,
)
