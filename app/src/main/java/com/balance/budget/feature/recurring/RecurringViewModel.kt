package com.balance.budget.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.data.repository.CategoryRepository
import com.balance.budget.data.repository.RecurringRepository
import com.balance.budget.domain.model.Category
import com.balance.budget.domain.model.Recurring
import com.balance.budget.domain.model.RecurringCadence
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringUiState(
    val items: List<Recurring> = emptyList(),
    val categories: List<Category> = emptyList(),
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val state: StateFlow<RecurringUiState> = combine(
        recurringRepository.observeAll(),
        categoryRepository.observeActive(),
    ) { items, cats -> RecurringUiState(items, cats) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringUiState())

    fun add(amountMinor: Long, categoryId: Long, note: String?, cadence: RecurringCadence, anchorDay: Int) =
        viewModelScope.launch { recurringRepository.add(amountMinor, categoryId, note, cadence, anchorDay) }

    fun update(recurring: Recurring) = viewModelScope.launch { recurringRepository.update(recurring) }

    fun setActive(recurring: Recurring, active: Boolean) =
        viewModelScope.launch { recurringRepository.setActive(recurring, active) }

    fun delete(recurring: Recurring) = viewModelScope.launch { recurringRepository.delete(recurring) }
}
