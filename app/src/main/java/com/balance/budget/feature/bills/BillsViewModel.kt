package com.balance.budget.feature.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.data.repository.CategoryRepository
import com.balance.budget.data.repository.RecurringRepository
import com.balance.budget.domain.model.Category
import com.balance.budget.domain.recurring.BillSchedule
import com.balance.budget.domain.recurring.BillsSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BillsUiState(
    val summary: BillsSummary = BillsSummary(0, 0, emptyList()),
    val categoriesById: Map<Long, Category> = emptyMap(),
)

@HiltViewModel
class BillsViewModel @Inject constructor(
    recurringRepository: RecurringRepository,
    categoryRepository: CategoryRepository,
    clock: () -> Long,
) : ViewModel() {

    val state: StateFlow<BillsUiState> = combine(
        recurringRepository.observeActive(),
        categoryRepository.observeActive(),
    ) { recurring, cats ->
        BillsUiState(
            summary = BillSchedule.compute(recurring, clock()),
            categoriesById = cats.associateBy { it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillsUiState())
}
