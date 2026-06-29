package com.balance.budget.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.balance.budget.data.local.entity.RecurringEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(recurring: RecurringEntity): Long

    @Update
    suspend fun update(recurring: RecurringEntity)

    @Delete
    suspend fun delete(recurring: RecurringEntity)

    @Query("SELECT * FROM recurring ORDER BY next_due_date ASC")
    fun observeAll(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring WHERE is_active = 1 ORDER BY next_due_date ASC")
    fun observeActive(): Flow<List<RecurringEntity>>

    /** Sum of all active recurring commitments (paise) — feeds safe-to-spend. */
    @Query("SELECT COALESCE(SUM(amount_minor), 0) FROM recurring WHERE is_active = 1")
    fun observeActiveTotal(): Flow<Long>

    @Query("SELECT * FROM recurring WHERE is_active = 1")
    suspend fun getActive(): List<RecurringEntity>

    /** Advances just the next-due-date (used by materialization; doesn't recompute). */
    @Query("UPDATE recurring SET next_due_date = :nextDueDate WHERE id = :id")
    suspend fun updateNextDueDate(id: Long, nextDueDate: Long)
}
