package com.balance.budget.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.balance.budget.data.local.entity.ExpenseTagEntity
import com.balance.budget.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY sort_order ASC, name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM tags")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)

    // --- expense ↔ tag links ------------------------------------------------

    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN expense_tags et ON et.tag_id = t.id
        WHERE et.expense_id = :expenseId
        ORDER BY t.sort_order ASC, t.name ASC
        """
    )
    fun observeTagsForExpense(expenseId: Long): Flow<List<TagEntity>>

    @Query("SELECT tag_id FROM expense_tags WHERE expense_id = :expenseId")
    suspend fun tagIdsForExpense(expenseId: Long): List<Long>

    @Query("DELETE FROM expense_tags WHERE expense_id = :expenseId")
    suspend fun clearTagsForExpense(expenseId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLinks(links: List<ExpenseTagEntity>)

    /** Replace the full tag set for one expense atomically. */
    @Transaction
    suspend fun setTagsForExpense(expenseId: Long, tagIds: List<Long>) {
        clearTagsForExpense(expenseId)
        if (tagIds.isNotEmpty()) {
            insertLinks(tagIds.map { ExpenseTagEntity(expenseId, it) })
        }
    }
}
