package com.balance.budget.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.balance.budget.data.local.entity.BudgetAdjustmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetAdjustmentDao {

    /** All moves recorded for one month (YYYYMM), oldest first. */
    @Query("SELECT * FROM budget_adjustments WHERE ym = :ym ORDER BY created_at ASC")
    fun observeForMonth(ym: Int): Flow<List<BudgetAdjustmentEntity>>

    @Query("SELECT * FROM budget_adjustments WHERE ym = :ym ORDER BY created_at ASC")
    suspend fun getForMonth(ym: Int): List<BudgetAdjustmentEntity>

    @Insert
    suspend fun insert(adjustment: BudgetAdjustmentEntity): Long

    @Query("DELETE FROM budget_adjustments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
