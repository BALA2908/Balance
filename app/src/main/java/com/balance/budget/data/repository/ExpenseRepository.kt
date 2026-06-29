package com.balance.budget.data.repository

import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.data.local.dao.ExpenseDao
import com.balance.budget.data.local.entity.ExpenseEntity
import com.balance.budget.domain.model.Expense
import com.balance.budget.domain.model.ExpenseDraft
import com.balance.budget.domain.model.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val clock: () -> Long, // injectable "now", keeps saves testable
) {

    /**
     * The single save path for a new expense — used by BOTH the FAB and the
     * deep-link Quick Add. There is exactly one place that writes a new expense.
     */
    suspend fun addExpense(draft: ExpenseDraft): Long {
        val entity = ExpenseEntity(
            amountMinor = draft.amountMinor,
            categoryId = draft.categoryId,
            note = draft.note?.trim()?.ifEmpty { null },
            timestamp = draft.timestamp,
            createdAt = clock(),
            source = draft.source,
            merchant = draft.merchant,
        )
        return expenseDao.insert(entity)
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.update(
            ExpenseEntity(
                id = expense.id,
                amountMinor = expense.amountMinor,
                categoryId = expense.categoryId,
                note = expense.note,
                timestamp = expense.timestamp,
                createdAt = expense.createdAt,
                source = expense.source,
                merchant = expense.merchant,
            )
        )
    }

    suspend fun deleteExpense(id: Long) = expenseDao.deleteById(id)

    suspend fun getById(id: Long): Expense? = expenseDao.getById(id)?.toDomain()

    fun observeRecent(limit: Int = 20): Flow<List<ExpenseWithCategory>> =
        expenseDao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    fun observeAll(): Flow<List<ExpenseWithCategory>> =
        expenseDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /** Searchable/filterable history (note/merchant text + optional category). */
    fun observeFiltered(query: String, categoryId: Long?): Flow<List<ExpenseWithCategory>> =
        expenseDao.observeFiltered(query.trim(), categoryId).map { rows -> rows.map { it.toDomain() } }

    fun observeForMonth(ym: YearMonth): Flow<List<ExpenseWithCategory>> =
        expenseDao.observeBetween(
            DateTimeUtil.startOfMonthMillis(ym),
            DateTimeUtil.endOfMonthMillis(ym),
        ).map { rows -> rows.map { it.toDomain() } }

    /** Total spend (paise) for a month — math stays in SQL/Kotlin, never AI. */
    fun observeMonthTotal(ym: YearMonth): Flow<Long> =
        expenseDao.observeTotalBetween(
            DateTimeUtil.startOfMonthMillis(ym),
            DateTimeUtil.endOfMonthMillis(ym),
        )

    fun observeCount(): Flow<Int> = expenseDao.observeCount()
}
