package com.balance.budget.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.data.preferences.SettingsRepository
import com.balance.budget.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * First-launch flow. Writes the overall monthly budget (so safe-to-spend has
 * something to calculate against) and marks onboarding complete. Skipping just
 * marks it complete — the dashboard then shows a "set a budget" prompt.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: () -> Long,
) : ViewModel() {

    fun complete(amountMinor: Long) = viewModelScope.launch {
        budgetRepository.setOverallBudget(amountMinor, DateTimeUtil.yearMonth(clock()))
        // New installs opt into rollover by default; existing installs never run
        // onboarding, so they keep the OFF default and their current behavior.
        settingsRepository.setRolloverEnabled(true)
        settingsRepository.setFirstLaunchComplete(true)
    }

    fun skip() = viewModelScope.launch {
        settingsRepository.setRolloverEnabled(true)
        settingsRepository.setFirstLaunchComplete(true)
    }
}
