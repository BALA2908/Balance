package com.balance.budget.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.balance.budget.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Query("SELECT * FROM budgets ORDER BY effective_from_ym DESC")
    fun observeAll(): Flow<List<BudgetEntity>>

    /**
     * The overall (category-less) budget in effect for the given year-month key.
     * That's the most recent row at or before the month.
     */
    @Query(
        """
        SELECT * FROM budgets
        WHERE category_id IS NULL AND effective_from_ym <= :ym
        ORDER BY effective_from_ym DESC LIMIT 1
        """
    )
    fun observeActiveOverall(ym: Int): Flow<BudgetEntity?>

    /** The per-category budget in effect for the given year-month key. */
    @Query(
        """
        SELECT * FROM budgets
        WHERE category_id = :categoryId AND effective_from_ym <= :ym
        ORDER BY effective_from_ym DESC LIMIT 1
        """
    )
    fun observeActiveForCategory(categoryId: Long, ym: Int): Flow<BudgetEntity?>

    /** All per-category budgets effective for a month (latest per category). */
    @Query(
        """
        SELECT b.* FROM budgets b
        WHERE b.category_id IS NOT NULL AND b.effective_from_ym <= :ym
        AND b.effective_from_ym = (
            SELECT MAX(b2.effective_from_ym) FROM budgets b2
            WHERE b2.category_id = b.category_id AND b2.effective_from_ym <= :ym
        )
        """
    )
    fun observeActiveCategoryBudgets(ym: Int): Flow<List<BudgetEntity>>
}
