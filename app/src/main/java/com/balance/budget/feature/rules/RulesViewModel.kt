package com.balance.budget.feature.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.data.categorize.Categorizer
import com.balance.budget.data.repository.CategoryRepository
import com.balance.budget.data.repository.CategoryRuleRepository
import com.balance.budget.domain.model.Category
import com.balance.budget.domain.model.CategoryRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LearnedSuggestion(val label: String, val categoryId: Long)

data class RulesUiState(
    val rules: List<CategoryRule> = emptyList(),
    val categories: List<Category> = emptyList(),
    val learned: List<LearnedSuggestion> = emptyList(),
) {
    val categoriesById: Map<Long, Category> get() = categories.associateBy { it.id }
}

/**
 * Backs the rules manager. Rules sit above the learned categorizer; this screen
 * makes the previously-silent learning visible: existing learned patterns are
 * offered for one-tap promotion into explicit, editable rules.
 */
@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: CategoryRuleRepository,
    categoryRepository: CategoryRepository,
    private val categorizer: Categorizer,
) : ViewModel() {

    private val learnedFlow = MutableStateFlow<List<LearnedSuggestion>>(emptyList())

    val state: StateFlow<RulesUiState> = combine(
        ruleRepository.observeAll(),
        categoryRepository.observeActive(),
        learnedFlow,
    ) { rules, cats, learned ->
        val rulePatterns = rules.map { it.pattern.lowercase().trim() }.toSet()
        val validIds = cats.map { it.id }.toSet()
        RulesUiState(
            rules = rules,
            categories = cats,
            // Only suggest learned labels not already a rule and whose category still exists.
            learned = learned.filter { it.label.lowercase().trim() !in rulePatterns && it.categoryId in validIds },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesUiState())

    init {
        refreshLearned()
    }

    private fun refreshLearned() = viewModelScope.launch {
        learnedFlow.value = categorizer.learnedPatterns().map { LearnedSuggestion(it.first, it.second) }
    }

    fun add(pattern: String, categoryId: Long) = viewModelScope.launch {
        ruleRepository.add(pattern, categoryId)
        refreshLearned()
    }

    fun update(rule: CategoryRule, pattern: String, categoryId: Long) = viewModelScope.launch {
        val cleaned = pattern.trim()
        if (cleaned.isEmpty()) return@launch
        ruleRepository.update(rule.copy(pattern = cleaned, categoryId = categoryId))
    }

    fun delete(rule: CategoryRule) = viewModelScope.launch { ruleRepository.delete(rule.id) }

    fun promote(suggestion: LearnedSuggestion) = viewModelScope.launch {
        ruleRepository.add(suggestion.label, suggestion.categoryId)
        refreshLearned()
    }
}
