package com.balance.budget.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.balance.budget.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ExpenseWithCategoryRow>>

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<ExpenseWithCategoryRow>>

    @Transaction
    @Query(
        """
        SELECT * FROM expenses
        WHERE timestamp BETWEEN :startMillis AND :endMillis
        ORDER BY timestamp DESC, id DESC
        """
    )
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategoryRow>>

    /** Total spend in a time window (paise). Null-safe: 0 when no rows. */
    @Query(
        """
        SELECT COALESCE(SUM(amount_minor), 0) FROM expenses
        WHERE timestamp BETWEEN :startMillis AND :endMillis
        """
    )
    fun observeTotalBetween(startMillis: Long, endMillis: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM expenses")
    fun observeCount(): Flow<Int>

    /**
     * Searchable/filterable history. Empty [query] matches all; [categoryId] null
     * matches all categories. Matches the query against note and merchant text.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM expenses
        WHERE (:query = '' OR note LIKE '%' || :query || '%' OR merchant LIKE '%' || :query || '%')
          AND (:categoryId IS NULL OR category_id = :categoryId)
        ORDER BY timestamp DESC, id DESC
        """
    )
    fun observeFiltered(query: String, categoryId: Long?): Flow<List<ExpenseWithCategoryRow>>
}
