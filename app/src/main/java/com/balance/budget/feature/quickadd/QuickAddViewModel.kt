package com.balance.budget.feature.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.data.categorize.Categorizer
import com.balance.budget.data.repository.CategoryRepository
import com.balance.budget.data.repository.ExpenseRepository
import com.balance.budget.domain.model.Category
import com.balance.budget.domain.model.ExpenseDraft
import com.balance.budget.domain.model.ExpenseSource
import com.balance.budget.core.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuickAddUiState(
    val amountInput: String = "",
    val amountMinor: Long = 0,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val note: String = "",
    val timestamp: Long = 0L,
    val source: ExpenseSource = ExpenseSource.MANUAL,
    val merchant: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    val canSave: Boolean get() = amountMinor > 0 && selectedCategoryId != null && !isSaving
}

/**
 * The single source of logic for adding an expense. Both the in-app FAB sheet
 * and the deep-link Quick Add Activity drive THIS view model and call THIS
 * [save] — there is no duplicated save logic anywhere.
 */
@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val categorizer: Categorizer,
    private val clock: () -> Long,
) : ViewModel() {

    private val _state = MutableStateFlow(QuickAddUiState(timestamp = clock()))
    val state: StateFlow<QuickAddUiState> = _state.asStateFlow()

    private companion object {
        const val MAX_INT_DIGITS = 9
    }

    init {
        viewModelScope.launch {
            categoryRepository.observeActive().collect { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
    }

    // --- Number pad input -------------------------------------------------

    fun onDigit(d: Int) {
        if (d !in 0..9) return
        _state.update { s ->
            val current = s.amountInput
            // Block extra leading zeros and cap precision/length.
            if (current == "0" && d == 0) return@update s
            val next = if (current == "0") d.toString() else current + d.toString()
            if (!withinLimits(next)) return@update s
            s.copyWithAmount(next)
        }
    }

    fun onDecimal() {
        _state.update { s ->
            if (s.amountInput.contains('.')) return@update s
            val next = if (s.amountInput.isEmpty()) "0." else s.amountInput + "."
            s.copyWithAmount(next)
        }
    }

    fun onBackspace() {
        _state.update { s ->
            if (s.amountInput.isEmpty()) return@update s
            s.copyWithAmount(s.amountInput.dropLast(1))
        }
    }

    fun clearAmount() = _state.update { it.copyWithAmount("") }

    private fun withinLimits(candidate: String): Boolean {
        val dot = candidate.indexOf('.')
        val intPart = if (dot >= 0) candidate.substring(0, dot) else candidate
        val fracPart = if (dot >= 0) candidate.substring(dot + 1) else ""
        return intPart.length <= MAX_INT_DIGITS && fracPart.length <= 2
    }

    private fun QuickAddUiState.copyWithAmount(input: String): QuickAddUiState =
        copy(amountInput = input, amountMinor = Money.parseToMinor(input) ?: 0L, error = null)

    // --- Other fields -----------------------------------------------------

    fun selectCategory(id: Long) = _state.update { it.copy(selectedCategoryId = id) }
    fun setNote(text: String) = _state.update { it.copy(note = text) }
    fun setTimestamp(epochMillis: Long) = _state.update { it.copy(timestamp = epochMillis) }

    /** Prefill from a deep link or an auto-imported notification (Phase 4). */
    fun prefill(amountMinor: Long?, note: String?, merchant: String?, source: ExpenseSource) {
        _state.update { s ->
            val input = amountMinor?.takeIf { it > 0 }?.let { Money.formatPlain(it).replace(",", "") }
            s.copy(
                amountInput = input ?: s.amountInput,
                amountMinor = amountMinor ?: s.amountMinor,
                note = note ?: s.note,
                merchant = merchant ?: s.merchant,
                source = source,
            )
        }
    }

    // --- Save (the one and only save path) --------------------------------

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                expenseRepository.addExpense(
                    ExpenseDraft(
                        amountMinor = s.amountMinor,
                        categoryId = s.selectedCategoryId!!,
                        note = s.note,
                        timestamp = s.timestamp,
                        source = s.source,
                        merchant = s.merchant,
                    )
                )
            }.onSuccess {
                // Teach the categorizer from this real choice (merchant beats note).
                categorizer.learn(s.merchant ?: s.note, s.selectedCategoryId!!)
                _state.update { it.copy(isSaving = false, saved = true) }
            }.onFailure { e ->
                _state.update { it.copy(isSaving = false, error = e.message ?: "Couldn't save") }
            }
        }
    }

    /** Reset after the save animation completes (used when reusing the sheet). */
    fun consumeSaved() {
        _state.value = QuickAddUiState(
            categories = _state.value.categories,
            timestamp = clock(),
        )
    }
}
