package com.balance.budget.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.balance.budget.data.local.entity.BalanceSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceSnapshotDao {

    @Query("SELECT * FROM account_balance_snapshots ORDER BY recorded_at ASC")
    fun observeAll(): Flow<List<BalanceSnapshotEntity>>

    @Insert
    suspend fun insert(snapshot: BalanceSnapshotEntity): Long

    @Query("DELETE FROM account_balance_snapshots WHERE id = :id")
    suspend fun deleteById(id: Long)
}
