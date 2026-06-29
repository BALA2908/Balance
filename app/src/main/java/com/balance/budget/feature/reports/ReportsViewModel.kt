package com.balance.budget.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.data.preferences.SettingsRepository
import com.balance.budget.data.repository.AnalyticsRepository
import com.balance.budget.data.repository.ExpenseRepository
import com.balance.budget.domain.ai.AgentService
import com.balance.budget.domain.ai.AiText
import com.balance.budget.domain.analytics.AnalyticsSnapshot
import com.balance.budget.domain.analytics.TrendAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

data class ReportsUiState(
    val snapshot: AnalyticsSnapshot,
    val dailyCumulative: List<Long>,
    val summary: AiText,
    val tips: AiText,
    val reduceMotion: Boolean,
    val isLoading: Boolean,
)

/**
 * Reports = the deterministic snapshot + a daily cumulative series for the trend
 * chart + the AI-written summary and tips (which gracefully fall back to warm
 * templates offline). Numbers come from the engine; the AI only phrases them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    analyticsRepository: AnalyticsRepository,
    expenseRepository: ExpenseRepository,
    settingsRepository: SettingsRepository,
    private val agentService: AgentService,
    private val clock: () -> Long,
) : ViewModel() {

    private val month: YearMonth = DateTimeUtil.yearMonth(clock())

    val state: StateFlow<ReportsUiState> = combine(
        analyticsRepository.snapshot,
        expenseRepository.observeForMonth(month),
        settingsRepository.reduceMotion,
    ) { snapshot, monthExpenses, reduceMotion -> Triple(snapshot, monthExpenses, reduceMotion) }
        .mapLatest { (snapshot, monthExpenses, reduceMotion) ->
            ReportsUiState(
                snapshot = snapshot,
                dailyCumulative = TrendAnalytics.dailyCumulative(monthExpenses, snapshot.month, clock()),
                summary = agentService.analystSummary(snapshot),
                tips = agentService.advisorTips(snapshot),
                reduceMotion = reduceMotion,
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReportsUiState(
                snapshot = AnalyticsSnapshot.empty(month),
                dailyCumulative = emptyList(),
                summary = AiText.deterministic(""),
                tips = AiText.deterministic(""),
                reduceMotion = false,
                isLoading = true,
            ),
        )
}
