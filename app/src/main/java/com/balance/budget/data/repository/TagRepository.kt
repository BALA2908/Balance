package com.balance.budget.data.repository

import com.balance.budget.data.local.dao.TagDao
import com.balance.budget.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao,
) {
    fun observeAll(): Flow<List<Tag>> =
        tagDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeTagsForExpense(expenseId: Long): Flow<List<Tag>> =
        tagDao.observeTagsForExpense(expenseId).map { list -> list.map { it.toDomain() } }

    suspend fun tagIdsForExpense(expenseId: Long): List<Long> = tagDao.tagIdsForExpense(expenseId)

    suspend fun add(name: String, colorHex: String): Long {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return -1
        return tagDao.insert(
            com.balance.budget.domain.model.Tag(
                id = 0, name = cleaned, colorHex = colorHex, sortOrder = tagDao.maxSortOrder() + 1,
            ).toEntity()
        )
    }

    suspend fun update(tag: Tag) = tagDao.update(tag.toEntity())

    suspend fun delete(id: Long) = tagDao.deleteById(id)

    /** Replace an expense's tags (used by the save path and the editor). */
    suspend fun setTagsForExpense(expenseId: Long, tagIds: List<Long>) =
        tagDao.setTagsForExpense(expenseId, tagIds)
}
