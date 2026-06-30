package com.balance.budget.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.balance.budget.data.local.entity.CategoryRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {

    @Query("SELECT * FROM category_rules ORDER BY sort_order ASC, id ASC")
    fun observeAll(): Flow<List<CategoryRuleEntity>>

    @Query("SELECT * FROM category_rules ORDER BY sort_order ASC, id ASC")
    suspend fun getAll(): List<CategoryRuleEntity>

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM category_rules")
    suspend fun maxSortOrder(): Int

    @Query("SELECT COUNT(*) FROM category_rules WHERE pattern = :pattern AND category_id = :categoryId")
    suspend fun countMatching(pattern: String, categoryId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(rule: CategoryRuleEntity): Long

    @Update
    suspend fun update(rule: CategoryRuleEntity)

    @Query("DELETE FROM category_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
