package com.balance.budget.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.data.preferences.SettingsRepository
import com.balance.budget.data.repository.AnalyticsRepository
import com.balance.budget.data.repository.ExpenseRepository
import com.balance.budget.domain.analytics.AnalyticsSnapshot
import com.balance.budget.domain.model.ExpenseWithCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

data class DashboardUiState(
    val snapshot: AnalyticsSnapshot,
    val recent: List<ExpenseWithCategory>,
    val reduceMotion: Boolean,
    val isLoading: Boolean,
) {
    val hasExpenses: Boolean get() = recent.isNotEmpty()
}

/**
 * Dashboard state = the deterministic [AnalyticsSnapshot] (safe-to-spend, budget
 * progress, category breakdown) plus the recent expenses list and the motion
 * preference. All numbers come from the engine; this VM does no math.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    analyticsRepository: AnalyticsRepository,
    expenseRepository: ExpenseRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<DashboardUiState> = combine(
        analyticsRepository.snapshot,
        expenseRepository.observeRecent(limit = 20),
        settingsRepository.reduceMotion,
    ) { snapshot, recent, reduceMotion ->
        DashboardUiState(snapshot, recent, reduceMotion, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(
            snapshot = AnalyticsSnapshot.empty(YearMonth.now(DateTimeUtil.zone)),
            recent = emptyList(),
            reduceMotion = false,
            isLoading = true,
        ),
    )
}
