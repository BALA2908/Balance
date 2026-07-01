package com.balance.budget.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.data.export.ExportManager
import com.balance.budget.data.preferences.SettingsRepository
import com.balance.budget.data.repository.ResetManager
import com.balance.budget.domain.model.ThemeMode
import com.balance.budget.notifications.NudgeScheduler
import com.balance.budget.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val reduceMotion: Boolean = false,
    val aiOnDevice: Boolean = true,
    val aiCloud: Boolean = false,
    val autoImport: Boolean = false,
    val rolloverEnabled: Boolean = false,
    val proactiveNudges: Boolean = false,
    val envelopeMode: Boolean = false,
    val monthlyIncomeMinor: Long? = null,
    val dailyReminderEnabled: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val exportManager: ExportManager,
    private val nudgeScheduler: NudgeScheduler,
    private val reminderScheduler: ReminderScheduler,
    private val resetManager: ResetManager,
) : ViewModel() {

    /** Emits a ready-to-share file (CSV/PDF), or an error message. */
    private val _exports = MutableSharedFlow<ExportManager.Export>(extraBufferCapacity = 1)
    val exports: SharedFlow<ExportManager.Export> = _exports.asSharedFlow()
    private val _exportError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val exportError: SharedFlow<String> = _exportError.asSharedFlow()

    // Five typed flows is combine()'s limit, so fold the sixth (rollover) in via a
    // second combine — keeps everything type-safe (no Array<Any?> erasure).
    private val baseState = combine(
        settings.themeMode,
        settings.reduceMotion,
        settings.aiOnDeviceEnabled,
        settings.aiCloudEnabled,
        settings.autoImportEnabled,
    ) { theme, reduceMotion, aiOnDevice, aiCloud, autoImport ->
        SettingsUiState(theme, reduceMotion, aiOnDevice, aiCloud, autoImport)
    }

    // combine() tops out at five typed flows, so fold the remaining toggles into
    // one holder first, then merge with the base state.
    private data class Extra(
        val nudges: Boolean,
        val envelope: Boolean,
        val income: Long?,
        val dailyReminder: Boolean,
    )

    private val extra = combine(
        settings.proactiveNudges,
        settings.envelopeMode,
        settings.monthlyIncomeMinor,
        settings.dailyReminderEnabled,
    ) { nudges, envelope, income, dailyReminder -> Extra(nudges, envelope, income, dailyReminder) }

    val state: StateFlow<SettingsUiState> = combine(
        baseState,
        settings.rolloverEnabled,
        extra,
    ) { base, rollover, e ->
        base.copy(
            rolloverEnabled = rollover,
            proactiveNudges = e.nudges,
            envelopeMode = e.envelope,
            monthlyIncomeMinor = e.income,
            dailyReminderEnabled = e.dailyReminder,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setReduceMotion(on: Boolean) = viewModelScope.launch { settings.setReduceMotion(on) }
    fun setAiOnDevice(on: Boolean) = viewModelScope.launch { settings.setAiOnDeviceEnabled(on) }
    fun setAiCloud(on: Boolean) = viewModelScope.launch { settings.setAiCloudEnabled(on) }
    fun setAutoImport(on: Boolean) = viewModelScope.launch { settings.setAutoImportEnabled(on) }
    fun setRolloverEnabled(on: Boolean) = viewModelScope.launch { settings.setRolloverEnabled(on) }
    fun setProactiveNudges(on: Boolean) = viewModelScope.launch {
        settings.setProactiveNudges(on)
        nudgeScheduler.setEnabled(on)
    }
    fun setEnvelopeMode(on: Boolean) = viewModelScope.launch { settings.setEnvelopeMode(on) }
    fun setMonthlyIncome(minor: Long) = viewModelScope.launch { settings.setMonthlyIncome(minor) }
    fun setDailyReminder(on: Boolean) = viewModelScope.launch {
        settings.setDailyReminderEnabled(on)
        reminderScheduler.setEnabled(on)
    }

    /**
     * Factory reset — erase everything and relaunch into onboarding. Destructive
     * and irreversible; the UI confirms before calling this.
     */
    fun resetApp() = viewModelScope.launch {
        resetManager.resetAll()
        resetManager.relaunch()
    }

    fun exportCsv() = export { exportManager.exportCsv() }
    fun exportPdf() = export { exportManager.exportPdf() }

    private fun export(block: suspend () -> ExportManager.Export) = viewModelScope.launch {
        runCatching { block() }
            .onSuccess { _exports.emit(it) }
            .onFailure { _exportError.emit("Couldn't export — ${it.message ?: "please try again"}") }
    }
}
