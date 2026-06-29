package com.balance.budget.data.repository

import com.balance.budget.data.local.dao.BudgetAdjustmentDao
import com.balance.budget.data.local.entity.BudgetAdjustmentEntity
import com.balance.budget.domain.analytics.BudgetAdjustment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The "move budget between categories" ledger. A move is recorded as one
 * append-only row for a month (YYYYMM key); reversing it = inserting the inverse.
 * The analytics engine reads these to reshape per-category effective budgets.
 */
@Singleton
class BudgetAdjustmentRepository @Inject constructor(
    private val dao: BudgetAdjustmentDao,
    private val clock: () -> Long,
) {
    fun observeForMonth(ymKey: Int): Flow<List<BudgetAdjustment>> =
        dao.observeForMonth(ymKey).map { list -> list.map { it.toDomain() } }

    suspend fun getForMonth(ymKey: Int): List<BudgetAdjustment> =
        dao.getForMonth(ymKey).map { it.toDomain() }

    /** Record a move of [amountMinor] from one category to another for [ymKey]. */
    suspend fun move(ymKey: Int, fromCategoryId: Long?, toCategoryId: Long?, amountMinor: Long) {
        if (amountMinor <= 0L) return
        dao.insert(
            BudgetAdjustmentEntity(
                ym = ymKey,
                fromCategoryId = fromCategoryId,
                toCategoryId = toCategoryId,
                amountMinor = amountMinor,
                createdAt = clock(),
            )
        )
    }
}
