package com.balance.budget.data.categorize

import com.balance.budget.data.repository.CategoryRuleRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a category for a merchant/note label, in priority order:
 *   1. an explicit user rule (always wins),
 *   2. the learned [Categorizer] memory,
 *   3. nothing (let the user pick).
 *
 * This makes the previously-silent categorizer tunable: a one-tap "promote" turns
 * a learned pattern into a visible, editable rule.
 */
@Singleton
class CategoryResolver @Inject constructor(
    private val rules: CategoryRuleRepository,
    private val categorizer: Categorizer,
) {
    suspend fun resolve(label: String?, validIds: Set<Long>): Long? {
        if (label.isNullOrBlank() || validIds.isEmpty()) return null
        return rules.matchCategory(label, validIds) ?: categorizer.suggest(label, validIds)
    }
}
