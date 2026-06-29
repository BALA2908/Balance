package com.balance.budget.feature.moneystory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.data.repository.AnalyticsRepository
import com.balance.budget.domain.ai.AgentService
import com.balance.budget.domain.story.MoneyStoryBuilder
import com.balance.budget.domain.story.StoryCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoneyStoryUiState(
    val cards: List<StoryCard> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class MoneyStoryViewModel @Inject constructor(
    private val analytics: AnalyticsRepository,
    private val agent: AgentService,
) : ViewModel() {

    private val _state = MutableStateFlow(MoneyStoryUiState())
    val state: StateFlow<MoneyStoryUiState> = _state.asStateFlow()

    private var narration: String? = null

    init {
        viewModelScope.launch {
            analytics.snapshot.collect { snapshot ->
                _state.update { it.copy(cards = MoneyStoryBuilder.build(snapshot, narration), isLoading = false) }
                // Fetch the AI narration once, then fold it into the deck.
                if (!snapshot.isEmpty && narration == null) {
                    narration = agent.analystSummary(snapshot).text
                    _state.update { it.copy(cards = MoneyStoryBuilder.build(snapshot, narration)) }
                }
            }
        }
    }
}
