package com.balance.budget.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.balance.budget.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User preferences, backed by Preferences DataStore. These are small, non-relational
 * settings — DataStore is the idiomatic fit, and it avoids a Room migration.
 *
 * Defaults reflect the product stance:
 *   - theme: DARK (dark-first cozy aesthetic)
 *   - on-device AI: ON (private, free); cloud AI: OFF (opt-in only)
 *   - auto-import: OFF (opt-in; sensitive notification permission)
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK
    }
    val currencyCode: Flow<String> = dataStore.data.map { it[KEY_CURRENCY] ?: DEFAULT_CURRENCY }
    val aiOnDeviceEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_AI_ON_DEVICE] ?: true }
    val aiCloudEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_AI_CLOUD] ?: false }
    val autoImportEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_IMPORT] ?: false }
    val reduceMotion: Flow<Boolean> = dataStore.data.map { it[KEY_REDUCE_MOTION] ?: false }
    val firstLaunchComplete: Flow<Boolean> = dataStore.data.map { it[KEY_FIRST_LAUNCH_DONE] ?: false }

    /**
     * Whether unspent category budget rolls forward into next month (and feeds
     * safe-to-spend). Default OFF so existing installs keep their current behavior;
     * onboarding turns it ON for new installs (see the onboarding completion).
     */
    val rolloverEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_ROLLOVER] ?: false }

    /** Opt-in proactive nudges (over-budget / unusual spend / off-track). Default OFF. */
    val proactiveNudges: Flow<Boolean> = dataStore.data.map { it[KEY_NUDGES] ?: false }

    /** Envelope (zero-based) mode: safe-to-spend is the sum of unspent category
     *  envelopes rather than the overall budget. Default OFF. */
    val envelopeMode: Flow<Boolean> = dataStore.data.map { it[KEY_ENVELOPE] ?: false }

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    suspend fun setCurrencyCode(code: String) = dataStore.edit { it[KEY_CURRENCY] = code }
    suspend fun setAiOnDeviceEnabled(enabled: Boolean) = dataStore.edit { it[KEY_AI_ON_DEVICE] = enabled }
    suspend fun setAiCloudEnabled(enabled: Boolean) = dataStore.edit { it[KEY_AI_CLOUD] = enabled }
    suspend fun setAutoImportEnabled(enabled: Boolean) = dataStore.edit { it[KEY_AUTO_IMPORT] = enabled }
    suspend fun setReduceMotion(enabled: Boolean) = dataStore.edit { it[KEY_REDUCE_MOTION] = enabled }
    suspend fun setFirstLaunchComplete(done: Boolean) = dataStore.edit { it[KEY_FIRST_LAUNCH_DONE] = done }
    suspend fun setRolloverEnabled(enabled: Boolean) = dataStore.edit { it[KEY_ROLLOVER] = enabled }
    suspend fun setProactiveNudges(enabled: Boolean) = dataStore.edit { it[KEY_NUDGES] = enabled }
    suspend fun setEnvelopeMode(enabled: Boolean) = dataStore.edit { it[KEY_ENVELOPE] = enabled }

    private companion object {
        const val DEFAULT_CURRENCY = "INR"
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_CURRENCY = stringPreferencesKey("currency_code")
        val KEY_AI_ON_DEVICE = booleanPreferencesKey("ai_on_device_enabled")
        val KEY_AI_CLOUD = booleanPreferencesKey("ai_cloud_enabled")
        val KEY_AUTO_IMPORT = booleanPreferencesKey("auto_import_enabled")
        val KEY_REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val KEY_FIRST_LAUNCH_DONE = booleanPreferencesKey("first_launch_complete")
        val KEY_ROLLOVER = booleanPreferencesKey("rollover_enabled")
        val KEY_NUDGES = booleanPreferencesKey("proactive_nudges")
        val KEY_ENVELOPE = booleanPreferencesKey("envelope_mode")
    }
}
