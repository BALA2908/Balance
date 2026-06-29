package com.balance.budget.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.balance.budget.data.local.entity.ImportCandidateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportCandidateDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(candidate: ImportCandidateEntity): Long

    @Query("SELECT * FROM import_candidates ORDER BY posted_at DESC, id DESC")
    fun observeAll(): Flow<List<ImportCandidateEntity>>

    @Query("SELECT COUNT(*) FROM import_candidates")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM import_candidates WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Dedupe guard: same amount within a short window is treated as the same txn. */
    @Query(
        "SELECT * FROM import_candidates WHERE amount_minor = :amountMinor " +
            "AND posted_at BETWEEN :fromMillis AND :toMillis LIMIT 1"
    )
    suspend fun findSimilar(amountMinor: Long, fromMillis: Long, toMillis: Long): ImportCandidateEntity?
}
