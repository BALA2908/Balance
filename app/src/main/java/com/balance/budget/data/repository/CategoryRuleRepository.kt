package com.balance.budget.data.repository

import com.balance.budget.data.local.dao.CategoryRuleDao
import com.balance.budget.domain.model.CategoryRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRuleRepository @Inject constructor(
    private val dao: CategoryRuleDao,
) {
    fun observeAll(): Flow<List<CategoryRule>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** First rule (by priority) whose pattern is contained in [label]; its category if valid. */
    suspend fun matchCategory(label: String?, validIds: Set<Long>): Long? {
        val needle = label?.lowercase()?.trim().orEmpty()
        if (needle.isEmpty()) return null
        return dao.getAll()
            .firstOrNull { it.categoryId in validIds && needle.contains(it.pattern.lowercase().trim()) }
            ?.categoryId
    }

    suspend fun add(pattern: String, categoryId: Long): Long {
        val cleaned = pattern.trim()
        if (cleaned.isEmpty()) return -1
        if (dao.countMatching(cleaned, categoryId) > 0) return -1 // no duplicates
        return dao.insert(
            CategoryRule(id = 0, pattern = cleaned, categoryId = categoryId, sortOrder = dao.maxSortOrder() + 1).toEntity()
        )
    }

    suspend fun update(rule: CategoryRule) = dao.update(rule.toEntity())

    suspend fun delete(id: Long) = dao.deleteById(id)
}
