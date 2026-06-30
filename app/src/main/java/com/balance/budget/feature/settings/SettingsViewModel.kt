package com.balance.budget.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.data.export.ExportManager
import com.balance.budget.data.preferences.SettingsRepository
import com.balance.budget.domain.model.ThemeMode
import com.balance.budget.notifications.NudgeScheduler
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
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val exportManager: ExportManager,
    private val nudgeScheduler: NudgeScheduler,
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

    val state: StateFlow<SettingsUiState> = combine(
        baseState,
        settings.rolloverEnabled,
        settings.proactiveNudges,
        settings.envelopeMode,
    ) { base, rollover, nudges, envelope ->
        base.copy(rolloverEnabled = rollover, proactiveNudges = nudges, envelopeMode = envelope)
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

    fun exportCsv() = export { exportManager.exportCsv() }
    fun exportPdf() = export { exportManager.exportPdf() }

    private fun export(block: suspend () -> ExportManager.Export) = viewModelScope.launch {
        runCatching { block() }
            .onSuccess { _exports.emit(it) }
            .onFailure { _exportError.emit("Couldn't export — ${it.message ?: "please try again"}") }
    }
}
