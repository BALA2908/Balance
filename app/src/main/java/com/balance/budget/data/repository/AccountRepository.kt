package com.balance.budget.data.repository

import com.balance.budget.data.local.dao.AccountDao
import com.balance.budget.data.local.seed.DefaultAccounts
import com.balance.budget.domain.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
) {
    fun observeActive(): Flow<List<Account>> =
        accountDao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeAll(): Flow<List<Account>> =
        accountDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Account? = accountDao.getById(id)?.toDomain()

    suspend fun getDefault(): Account? = accountDao.getDefault()?.toDomain()

    suspend fun add(account: Account): Long = accountDao.insert(account.toEntity())

    suspend fun update(account: Account) = accountDao.update(account.toEntity())

    suspend fun nextSortOrder(): Int = accountDao.maxSortOrder() + 1

    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> accountDao.updateSortOrder(id, index) }
    }

    /** Make [id] the one default account (exactly one default at a time). */
    suspend fun setDefault(id: Long) {
        accountDao.clearDefault()
        accountDao.getById(id)?.let { accountDao.update(it.copy(isDefault = true)) }
    }

    /** Seeds the default wallets exactly once (no-op if any already exist). */
    suspend fun ensureSeeded() {
        if (accountDao.count() == 0) {
            accountDao.insertAll(DefaultAccounts.list)
        }
    }
}
