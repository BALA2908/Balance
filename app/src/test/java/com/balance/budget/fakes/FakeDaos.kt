package com.balance.budget.fakes

import com.balance.budget.data.local.dao.CategoryDao
import com.balance.budget.data.local.dao.ExpenseDao
import com.balance.budget.data.local.dao.ExpenseWithCategoryRow
import com.balance.budget.data.local.entity.CategoryEntity
import com.balance.budget.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fakes for the DAOs, used by JVM unit tests. They implement just
 * enough behavior to exercise the repositories and view models without a device.
 */

class FakeCategoryDao(initial: List<CategoryEntity> = emptyList()) : CategoryDao {
    private val items = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeAll(): Flow<List<CategoryEntity>> = items
    override fun observeActive(): Flow<List<CategoryEntity>> =
        items.map { list -> list.filter { !it.isArchived } }

    override suspend fun getById(id: Long): CategoryEntity? = items.value.firstOrNull { it.id == id }
    override suspend fun count(): Int = items.value.size

    /** Non-suspend lookup for use inside non-suspend Flow operators in tests. */
    fun currentById(id: Long): CategoryEntity? = items.value.firstOrNull { it.id == id }

    override suspend fun insert(category: CategoryEntity): Long {
        val withId = category.copy(id = nextId++)
        items.value = items.value + withId
        return withId.id
    }

    override suspend fun insertAll(categories: List<CategoryEntity>): List<Long> =
        categories.map { insert(it) }

    override suspend fun update(category: CategoryEntity) {
        items.value = items.value.map { if (it.id == category.id) category else it }
    }

    override suspend fun updateSortOrder(id: Long, sortOrder: Int) {
        items.value = items.value.map { if (it.id == id) it.copy(sortOrder = sortOrder) else it }
    }

    override suspend fun maxSortOrder(): Int = items.value.maxOfOrNull { it.sortOrder } ?: -1
}

class FakeExpenseDao(
    private val categoryDao: FakeCategoryDao,
) : ExpenseDao {
    val inserted = mutableListOf<ExpenseEntity>()
    private val items = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(expense: ExpenseEntity): Long {
        val withId = expense.copy(id = nextId++)
        inserted += withId
        items.value = items.value + withId
        return withId.id
    }

    override suspend fun update(expense: ExpenseEntity) {
        items.value = items.value.map { if (it.id == expense.id) expense else it }
    }

    override suspend fun delete(expense: ExpenseEntity) {
        items.value = items.value.filterNot { it.id == expense.id }
    }

    override suspend fun deleteById(id: Long) {
        items.value = items.value.filterNot { it.id == id }
    }

    override suspend fun getById(id: Long): ExpenseEntity? = items.value.firstOrNull { it.id == id }

    private fun join(list: List<ExpenseEntity>): List<ExpenseWithCategoryRow> =
        list.mapNotNull { e ->
            categoryDao.currentById(e.categoryId)?.let { ExpenseWithCategoryRow(e, it) }
        }

    override fun observeRecent(limit: Int): Flow<List<ExpenseWithCategoryRow>> =
        items.map { join(it.sortedByDescending { e -> e.timestamp }.take(limit)) }

    override fun observeAll(): Flow<List<ExpenseWithCategoryRow>> =
        items.map { join(it.sortedByDescending { e -> e.timestamp }) }

    override fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategoryRow>> =
        items.map { join(it.filter { e -> e.timestamp in startMillis..endMillis }) }

    override fun observeFiltered(query: String, categoryId: Long?): Flow<List<ExpenseWithCategoryRow>> =
        items.map { list ->
            join(
                list.filter { e ->
                    (query.isEmpty() ||
                        e.note?.contains(query, ignoreCase = true) == true ||
                        e.merchant?.contains(query, ignoreCase = true) == true) &&
                        (categoryId == null || e.categoryId == categoryId)
                }.sortedByDescending { it.timestamp }
            )
        }

    override fun observeTotalBetween(startMillis: Long, endMillis: Long): Flow<Long> =
        items.map { list -> list.filter { it.timestamp in startMillis..endMillis }.sumOf { it.amountMinor } }

    override fun observeCount(): Flow<Int> = items.map { it.size }
}
