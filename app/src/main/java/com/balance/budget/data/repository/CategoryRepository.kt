package com.balance.budget.data.repository

import com.balance.budget.data.local.dao.CategoryDao
import com.balance.budget.data.local.seed.DefaultCategories
import com.balance.budget.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
) {
    fun observeActive(): Flow<List<Category>> =
        categoryDao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeAll(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Category? = categoryDao.getById(id)?.toDomain()

    suspend fun update(category: Category) = categoryDao.update(category.toEntity())

    suspend fun add(category: Category): Long = categoryDao.insert(category.toEntity())

    /** Next free sort position, so a newly created category lands at the end. */
    suspend fun nextSortOrder(): Int = categoryDao.maxSortOrder() + 1

    /**
     * Persist a new ordering. [orderedIds] is the full list of category ids in the
     * order the user dragged them into; each row's sort_order becomes its index.
     */
    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> categoryDao.updateSortOrder(id, index) }
    }

    /** Seeds the default categories exactly once (no-op if any already exist). */
    suspend fun ensureSeeded() {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(DefaultCategories.list)
        }
    }
}
