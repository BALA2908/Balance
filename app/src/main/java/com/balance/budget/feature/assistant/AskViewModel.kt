package com.balance.budget.feature.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.data.repository.AnalyticsRepository
import com.balance.budget.domain.ai.AgentService
import com.balance.budget.domain.ai.AiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AskUiState(
    val question: String = "",
    val answer: AiText? = null,
    val forecast: AiText? = null,
    val thinking: Boolean = false,
)

/**
 * Natural-language "can I afford ₹X this month?" + forecast. The verdict and all
 * figures are computed deterministically by [AgentService]/the engine; the AI (if
 * present) only rephrases. Works offline via the deterministic fallback.
 */
@HiltViewModel
class AskViewModel @Inject constructor(
    private val analytics: AnalyticsRepository,
    private val agent: AgentService,
) : ViewModel() {

    private val _state = MutableStateFlow(AskUiState())
    val state: StateFlow<AskUiState> = _state.asStateFlow()

    init {
        // Keep the analytics snapshot warm and surface a forecast as the default insight.
        viewModelScope.launch {
            analytics.snapshot.collect { snapshot ->
                if (_state.value.forecast == null && !snapshot.isEmpty) {
                    _state.update { it.copy(forecast = agent.forecast(snapshot)) }
                }
            }
        }
    }

    fun setQuestion(text: String) = _state.update { it.copy(question = text) }

    fun askQuick(text: String) {
        _state.update { it.copy(question = text) }
        ask()
    }

    fun ask() {
        val q = _state.value.question.trim()
        if (q.isEmpty() || _state.value.thinking) return
        _state.update { it.copy(thinking = true, answer = null) }
        viewModelScope.launch {
            val answer = agent.answerQuestion(analytics.snapshot.value, q)
            _state.update { it.copy(answer = answer, thinking = false) }
        }
    }
}
