package com.balance.budget.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.data.repository.SavingsGoalRepository
import com.balance.budget.domain.model.SavingsGoal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavingsGoalsViewModel @Inject constructor(
    private val repository: SavingsGoalRepository,
) : ViewModel() {

    val goals: StateFlow<List<SavingsGoal>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String, iconKey: String, colorHex: String, targetMinor: Long) = viewModelScope.launch {
        repository.add(name, iconKey, colorHex, targetMinor)
    }

    fun save(goal: SavingsGoal, name: String, iconKey: String, colorHex: String, targetMinor: Long) =
        viewModelScope.launch {
            val cleaned = name.trim()
            if (cleaned.isEmpty() || targetMinor <= 0) return@launch
            repository.update(goal.copy(name = cleaned, iconKey = iconKey, colorHex = colorHex, targetMinor = targetMinor))
        }

    fun contribute(id: Long, deltaMinor: Long) = viewModelScope.launch { repository.contribute(id, deltaMinor) }

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
}
