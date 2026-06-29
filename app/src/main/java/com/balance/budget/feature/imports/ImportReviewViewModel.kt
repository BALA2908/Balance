package com.balance.budget.feature.imports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.data.categorize.Categorizer
import com.balance.budget.data.repository.CategoryRepository
import com.balance.budget.data.repository.ExpenseRepository
import com.balance.budget.data.repository.ImportCandidateRepository
import com.balance.budget.domain.model.Category
import com.balance.budget.domain.model.ExpenseDraft
import com.balance.budget.domain.model.ExpenseSource
import com.balance.budget.domain.model.ImportCandidate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportReviewUiState(
    val candidates: List<ImportCandidate> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && candidates.isEmpty()
}

/**
 * The review queue for auto-imported transactions. Confirming routes through the
 * single expense writer (source = IMPORT) and teaches the categorizer; dismissing
 * just discards the candidate. Nothing here ever wrote an expense on its own.
 */
@HiltViewModel
class ImportReviewViewModel @Inject constructor(
    private val importCandidates: ImportCandidateRepository,
    private val expenseRepository: ExpenseRepository,
    private val categorizer: Categorizer,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val state: StateFlow<ImportReviewUiState> = combine(
        importCandidates.observeAll(),
        categoryRepository.observeActive(),
    ) { candidates, categories ->
        ImportReviewUiState(candidates, categories, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ImportReviewUiState())

    fun confirm(candidate: ImportCandidate, categoryId: Long) = viewModelScope.launch {
        expenseRepository.addExpense(
            ExpenseDraft(
                amountMinor = candidate.amountMinor,
                categoryId = categoryId,
                note = candidate.merchant,
                timestamp = candidate.postedAt,
                source = ExpenseSource.IMPORT,
                merchant = candidate.merchant,
            )
        )
        categorizer.learn(candidate.merchant, categoryId)
        importCandidates.remove(candidate.id)
    }

    fun dismiss(candidate: ImportCandidate) = viewModelScope.launch {
        importCandidates.remove(candidate.id)
    }
}
