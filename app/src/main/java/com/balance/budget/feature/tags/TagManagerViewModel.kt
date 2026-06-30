package com.balance.budget.feature.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.data.repository.ExpenseRepository
import com.balance.budget.data.repository.TagRepository
import com.balance.budget.domain.analytics.TripRecap
import com.balance.budget.domain.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagManagerViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    val tags: StateFlow<List<Tag>> = tagRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Live cross-category recap for one tag (deterministic; engine does the math). */
    fun recapFor(tagId: Long): Flow<TripRecap> =
        expenseRepository.observeForTag(tagId).map { TripRecap.from(it) }

    fun create(name: String, colorHex: String) = viewModelScope.launch {
        tagRepository.add(name, colorHex)
    }

    fun rename(tag: Tag, name: String, colorHex: String) = viewModelScope.launch {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return@launch
        tagRepository.update(tag.copy(name = cleaned, colorHex = colorHex))
    }

    fun delete(tag: Tag) = viewModelScope.launch { tagRepository.delete(tag.id) }
}
