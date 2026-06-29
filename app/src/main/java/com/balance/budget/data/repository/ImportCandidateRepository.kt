package com.balance.budget.data.repository

import com.balance.budget.data.local.dao.ImportCandidateDao
import com.balance.budget.data.local.entity.ImportCandidateEntity
import com.balance.budget.domain.model.ImportCandidate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Staging queue for auto-imported transactions awaiting user review. */
@Singleton
class ImportCandidateRepository @Inject constructor(
    private val dao: ImportCandidateDao,
    private val clock: () -> Long,
) {
    fun observeAll(): Flow<List<ImportCandidate>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeCount(): Flow<Int> = dao.observeCount()

    /**
     * Stages a parsed transaction. De-duplicates against the same amount within a
     * 2-minute window (UPI apps often post multiple notifications per txn).
     * Returns the new row id, or -1 if it was a duplicate.
     */
    suspend fun add(
        amountMinor: Long,
        merchant: String?,
        rawText: String,
        sourceApp: String,
        postedAt: Long,
        suggestedCategoryId: Long?,
    ): Long {
        val window = 2 * 60 * 1000L
        if (dao.findSimilar(amountMinor, postedAt - window, postedAt + window) != null) return -1
        return dao.insert(
            ImportCandidateEntity(
                amountMinor = amountMinor,
                merchant = merchant,
                rawText = rawText,
                sourceApp = sourceApp,
                postedAt = postedAt,
                createdAt = clock(),
                suggestedCategoryId = suggestedCategoryId,
            )
        )
    }

    suspend fun remove(id: Long) = dao.deleteById(id)
}
