package com.balance.budget.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.balance.budget.data.local.dao.BudgetAdjustmentDao
import com.balance.budget.data.local.dao.BudgetDao
import com.balance.budget.data.local.dao.CategoryDao
import com.balance.budget.data.local.dao.ExpenseDao
import com.balance.budget.data.local.dao.ImportCandidateDao
import com.balance.budget.data.local.dao.RecurringDao
import com.balance.budget.data.local.entity.BudgetAdjustmentEntity
import com.balance.budget.data.local.entity.BudgetEntity
import com.balance.budget.data.local.entity.CategoryEntity
import com.balance.budget.data.local.entity.ExpenseEntity
import com.balance.budget.data.local.entity.ImportCandidateEntity
import com.balance.budget.data.local.entity.RecurringEntity

@Database(
    entities = [
        CategoryEntity::class,
        ExpenseEntity::class,
        BudgetEntity::class,
        RecurringEntity::class,
        ImportCandidateEntity::class,
        BudgetAdjustmentEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao
    abstract fun importCandidateDao(): ImportCandidateDao
    abstract fun budgetAdjustmentDao(): BudgetAdjustmentDao

    companion object {
        const val NAME = "balance.db"
    }
}
