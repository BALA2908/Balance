package com.balance.budget.data.repository

import com.balance.budget.data.local.dao.BalanceSnapshotDao
import com.balance.budget.data.local.entity.BalanceSnapshotEntity
import com.balance.budget.domain.model.BalanceSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalanceSnapshotRepository @Inject constructor(
    private val dao: BalanceSnapshotDao,
    private val clock: () -> Long,
) {
    fun observeAll(): Flow<List<BalanceSnapshot>> =
        dao.observeAll().map { list -> list.map { BalanceSnapshot(it.id, it.recordedAt, it.netWorthMinor) } }

    /** Capture the current total net worth as a dated snapshot. */
    suspend fun record(netWorthMinor: Long): Long =
        dao.insert(BalanceSnapshotEntity(recordedAt = clock(), netWorthMinor = netWorthMinor))

    suspend fun delete(id: Long) = dao.deleteById(id)
}
