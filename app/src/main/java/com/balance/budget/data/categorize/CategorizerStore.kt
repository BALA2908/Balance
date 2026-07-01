package com.balance.budget.data.categorize

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// Dedicated DataStore for the learned label→category counts (small key/value data).
private val Context.categorizerDataStore: DataStore<Preferences> by preferencesDataStore(name = "categorizer")

/**
 * Persists how often a given label (a normalized merchant or note) was assigned
 * to each category. Small, non-relational learning data — DataStore, not Room.
 * Value encoding per label: "categoryId:count,categoryId:count".
 */
@Singleton
class CategorizerStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : LabelCountStore {
    override suspend fun counts(normalizedLabel: String): Map<Long, Int> {
        val raw = context.categorizerDataStore.data.first()[key(normalizedLabel)] ?: return emptyMap()
        return decode(raw)
    }

    override suspend fun increment(normalizedLabel: String, categoryId: Long) {
        context.categorizerDataStore.edit { prefs ->
            val current = decode(prefs[key(normalizedLabel)] ?: "").toMutableMap()
            current[categoryId] = (current[categoryId] ?: 0) + 1
            prefs[key(normalizedLabel)] = encode(current)
        }
    }

    override suspend fun allLearned(): Map<String, Map<Long, Int>> {
        val all = context.categorizerDataStore.data.first()
        return all.asMap().entries
            .filter { it.key.name.startsWith(PREFIX) && it.value is String }
            .associate { it.key.name.removePrefix(PREFIX) to decode(it.value as String) }
    }

    /** Forget everything learned — used by the factory reset. */
    suspend fun clearAll() {
        context.categorizerDataStore.edit { it.clear() }
    }

    private fun key(label: String) = stringPreferencesKey("$PREFIX$label")

    companion object {
        private const val PREFIX = "lbl_"

        fun encode(counts: Map<Long, Int>): String =
            counts.entries.joinToString(",") { "${it.key}:${it.value}" }

        fun decode(raw: String): Map<Long, Int> =
            raw.split(",")
                .mapNotNull { part ->
                    val bits = part.split(":")
                    if (bits.size != 2) return@mapNotNull null
                    val id = bits[0].toLongOrNull()
                    val count = bits[1].toIntOrNull()
                    if (id != null && count != null) id to count else null
                }
                .toMap()
    }
}
