package com.balance.budget.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.data.repository.CategoryRepository
import com.balance.budget.data.repository.ExpenseRepository
import com.balance.budget.domain.model.Category
import com.balance.budget.domain.model.Expense
import com.balance.budget.domain.model.ExpenseDraft
import com.balance.budget.domain.model.ExpenseWithCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A day's worth of expenses, for the grouped History list. */
data class HistoryDay(
    val label: String,
    val items: List<ExpenseWithCategory>,
    val totalMinor: Long,
)

data class HistoryUiState(
    val query: String = "",
    val selectedCategoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val days: List<HistoryDay> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && days.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val categoryFilter = MutableStateFlow<Long?>(null)

    val state: StateFlow<HistoryUiState> = combine(
        query, categoryFilter, categoryRepository.observeActive(),
    ) { q, cat, cats -> Triple(q, cat, cats) }
        .flatMapLatest { (q, cat, cats) ->
            expenseRepository.observeFiltered(q, cat).map { list ->
                HistoryUiState(
                    query = q,
                    selectedCategoryId = cat,
                    categories = cats,
                    days = groupByDay(list),
                    isLoading = false,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setQuery(q: String) { query.value = q }
    fun setCategoryFilter(id: Long?) { categoryFilter.value = id }

    fun update(expense: Expense) = viewModelScope.launch {
        expenseRepository.updateExpense(expense)
    }

    fun delete(item: ExpenseWithCategory) = viewModelScope.launch {
        expenseRepository.deleteExpense(item.expense.id)
    }

    /** Re-inserts a deleted expense (used for swipe-to-delete undo). */
    fun restore(item: ExpenseWithCategory) = viewModelScope.launch {
        val e = item.expense
        expenseRepository.addExpense(
            ExpenseDraft(
                amountMinor = e.amountMinor,
                categoryId = e.categoryId,
                note = e.note,
                timestamp = e.timestamp,
                source = e.source,
                merchant = e.merchant,
            )
        )
    }

    private fun groupByDay(items: List<ExpenseWithCategory>): List<HistoryDay> =
        items.groupBy { DateTimeUtil.localDate(it.expense.timestamp) }
            .map { (_, dayItems) ->
                HistoryDay(
                    label = DateTimeUtil.friendlyDate(dayItems.first().expense.timestamp),
                    items = dayItems,
                    totalMinor = dayItems.sumOf { it.expense.amountMinor },
                )
            }
}
