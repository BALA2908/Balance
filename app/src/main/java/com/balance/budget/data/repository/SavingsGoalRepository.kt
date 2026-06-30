package com.balance.budget.data.repository

import com.balance.budget.data.local.dao.SavingsGoalDao
import com.balance.budget.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavingsGoalRepository @Inject constructor(
    private val dao: SavingsGoalDao,
) {
    fun observeAll(): Flow<List<SavingsGoal>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun add(name: String, iconKey: String, colorHex: String, targetMinor: Long): Long {
        val cleaned = name.trim()
        if (cleaned.isEmpty() || targetMinor <= 0) return -1
        return dao.insert(
            SavingsGoal(
                id = 0, name = cleaned, iconKey = iconKey, colorHex = colorHex,
                targetMinor = targetMinor, savedMinor = 0, sortOrder = dao.maxSortOrder() + 1,
            ).toEntity()
        )
    }

    suspend fun update(goal: SavingsGoal) = dao.update(goal.toEntity())

    /** Add (or subtract, when negative) a contribution; clamped at zero. */
    suspend fun contribute(id: Long, deltaMinor: Long) = dao.addToSaved(id, deltaMinor)

    suspend fun delete(id: Long) = dao.deleteById(id)
}
