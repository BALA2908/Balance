package com.balance.budget.data.repository

import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.data.local.dao.BudgetAdjustmentDao
import com.balance.budget.data.local.dao.BudgetDao
import com.balance.budget.data.local.entity.BudgetEntity
import com.balance.budget.domain.model.Budget
import com.balance.budget.domain.model.BudgetPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val budgetAdjustmentDao: BudgetAdjustmentDao,
) {
    fun observeOverallBudget(ym: YearMonth): Flow<Budget?> =
        budgetDao.observeActiveOverall(DateTimeUtil.yearMonthKey(ym))
            .map { it?.toDomain() }

    fun observeCategoryBudget(categoryId: Long, ym: YearMonth): Flow<Budget?> =
        budgetDao.observeActiveForCategory(categoryId, DateTimeUtil.yearMonthKey(ym))
            .map { it?.toDomain() }

    fun observeCategoryBudgets(ym: YearMonth): Flow<List<Budget>> =
        budgetDao.observeActiveCategoryBudgets(DateTimeUtil.yearMonthKey(ym))
            .map { list -> list.map { it.toDomain() } }

    /** Set/replace the overall budget effective from the given month. */
    suspend fun setOverallBudget(amountMinor: Long, effectiveFrom: YearMonth) {
        budgetDao.insert(
            BudgetEntity(
                categoryId = null,
                amountMinor = amountMinor,
                period = BudgetPeriod.MONTHLY,
                effectiveFromYearMonth = DateTimeUtil.yearMonthKey(effectiveFrom),
            )
        )
    }

    suspend fun setCategoryBudget(categoryId: Long, amountMinor: Long, effectiveFrom: YearMonth) {
        budgetDao.insert(
            BudgetEntity(
                categoryId = categoryId,
                amountMinor = amountMinor,
                period = BudgetPeriod.MONTHLY,
                effectiveFromYearMonth = DateTimeUtil.yearMonthKey(effectiveFrom),
            )
        )
    }

    /**
     * Remove all budgets — the overall limit and every per-category limit, across
     * all months (budgets are versioned, so a true unset clears the history too).
     * Also drops any recorded budget moves. Expenses are untouched.
     */
    suspend fun clearAllBudgets() {
        budgetDao.deleteAll()
        budgetAdjustmentDao.deleteAll()
    }

    /** Remove the limit for a single category (all months). Expenses untouched. */
    suspend fun clearCategoryBudget(categoryId: Long) {
        budgetDao.deleteForCategory(categoryId)
    }
}
