package com.balance.budget.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.data.repository.CategoryRepository
import com.balance.budget.domain.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryManagerUiState(
    val active: List<Category> = emptyList(),
    val archived: List<Category> = emptyList(),
) {
    /** Guards against archiving your last category (Quick Add needs at least one). */
    val canArchive: Boolean get() = active.size > 1
}

/**
 * Backs the category manager: create, rename, recolor, re-icon, archive/restore,
 * and reorder. Archiving is a soft delete (we never hard-delete a category that
 * historical expenses still point at) — archived categories drop out of Quick Add
 * and budgets but their past expenses keep a valid reference.
 */
@HiltViewModel
class CategoryManagerViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val state: StateFlow<CategoryManagerUiState> = categoryRepository.observeAll()
        .map { all ->
            CategoryManagerUiState(
                active = all.filter { !it.isArchived },
                archived = all.filter { it.isArchived },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryManagerUiState())

    fun create(name: String, iconKey: String, colorHex: String) = viewModelScope.launch {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return@launch
        categoryRepository.add(
            Category(
                id = 0,
                name = cleaned,
                iconKey = iconKey,
                colorHex = colorHex,
                isDefault = false,
                isArchived = false,
                sortOrder = categoryRepository.nextSortOrder(),
            )
        )
    }

    fun save(category: Category, name: String, iconKey: String, colorHex: String) = viewModelScope.launch {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return@launch
        categoryRepository.update(category.copy(name = cleaned, iconKey = iconKey, colorHex = colorHex))
    }

    fun setArchived(category: Category, archived: Boolean) = viewModelScope.launch {
        if (archived && !state.value.canArchive) return@launch
        categoryRepository.update(category.copy(isArchived = archived))
    }

    /** Persist a new active-list ordering (full list of ids in display order). */
    fun reorder(orderedIds: List<Long>) = viewModelScope.launch {
        categoryRepository.reorder(orderedIds)
    }
}
