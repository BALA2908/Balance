package com.balance.budget.data.categorize

import javax.inject.Inject
import javax.inject.Singleton

/** Persistence boundary for the categorizer's learned counts (fakeable in tests). */
interface LabelCountStore {
    suspend fun counts(normalizedLabel: String): Map<Long, Int>
    suspend fun increment(normalizedLabel: String, categoryId: Long)

    /** All learned labels → their category counts (for "promote to rule"). */
    suspend fun allLearned(): Map<String, Map<Long, Int>> = emptyMap()
}

/**
 * Deterministic, learning categorizer. It remembers which category you assign to
 * a given label (merchant or note) and suggests the most frequent match next
 * time — so auto-imported merchants and repeat manual notes pre-select correctly,
 * and it gets better the more you use it. Learning happens through the single
 * save path (QuickAddViewModel) and on confirmed imports.
 *
 * (An on-device AI enhancer via AiProvider.suggestCategory can layer on top later
 * for unseen labels; the deterministic memory is the reliable base.)
 */
@Singleton
class Categorizer @Inject constructor(
    private val store: LabelCountStore,
) {
    /** Best category for a label among the currently valid categories, or null. */
    suspend fun suggest(label: String?, validIds: Set<Long>): Long? {
        val normalized = normalize(label) ?: return null
        return store.counts(normalized)
            .filterKeys { it in validIds }
            .maxByOrNull { it.value }
            ?.key
    }

    /** Reinforces label → category from a real user choice. */
    suspend fun learn(label: String?, categoryId: Long) {
        val normalized = normalize(label) ?: return
        store.increment(normalized, categoryId)
    }

    /** Learned label → its winning category id (for the "promote to rule" list). */
    suspend fun learnedPatterns(): List<Pair<String, Long>> =
        store.allLearned().mapNotNull { (label, counts) ->
            counts.maxByOrNull { it.value }?.let { label to it.key }
        }

    companion object {
        /** Lowercased, whitespace-collapsed key; null for blank/absent labels. */
        fun normalize(label: String?): String? =
            label?.lowercase()?.trim()?.replace(Regex("\\s+"), " ")?.takeIf { it.isNotBlank() }
    }
}
