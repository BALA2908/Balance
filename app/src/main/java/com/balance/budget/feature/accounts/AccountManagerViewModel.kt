package com.balance.budget.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.data.repository.AccountRepository
import com.balance.budget.domain.model.Account
import com.balance.budget.domain.model.AccountType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountManagerUiState(
    val active: List<Account> = emptyList(),
    val archived: List<Account> = emptyList(),
) {
    /** Need at least one wallet for Quick Add to assign. */
    val canArchive: Boolean get() = active.size > 1
}

/**
 * Backs the accounts manager: create, rename, recolour, re-icon, change type,
 * set-default, archive/restore, and reorder. Exactly one account is the default
 * (what Quick Add pre-selects). Archiving is a soft delete — past expenses keep a
 * valid (or SET NULL) reference. The default account can't be archived.
 */
@HiltViewModel
class AccountManagerViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    val state: StateFlow<AccountManagerUiState> = accountRepository.observeAll()
        .map { all ->
            AccountManagerUiState(
                active = all.filter { !it.isArchived },
                archived = all.filter { it.isArchived },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountManagerUiState())

    fun create(name: String, type: AccountType, iconKey: String, colorHex: String, balanceMinor: Long?) = viewModelScope.launch {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return@launch
        accountRepository.add(
            Account(
                id = 0,
                name = cleaned,
                type = type,
                iconKey = iconKey,
                colorHex = colorHex,
                openingBalanceMinor = balanceMinor,
                isDefault = false,
                isArchived = false,
                sortOrder = accountRepository.nextSortOrder(),
            )
        )
    }

    fun save(account: Account, name: String, type: AccountType, iconKey: String, colorHex: String, balanceMinor: Long?) =
        viewModelScope.launch {
            val cleaned = name.trim()
            if (cleaned.isEmpty()) return@launch
            accountRepository.update(
                account.copy(name = cleaned, type = type, iconKey = iconKey, colorHex = colorHex, openingBalanceMinor = balanceMinor),
            )
        }

    fun setDefault(account: Account) = viewModelScope.launch {
        accountRepository.setDefault(account.id)
    }

    fun setArchived(account: Account, archived: Boolean) = viewModelScope.launch {
        // Never archive the default wallet or the last remaining one.
        if (archived && (account.isDefault || !state.value.canArchive)) return@launch
        accountRepository.update(account.copy(isArchived = archived))
    }

    fun reorder(orderedIds: List<Long>) = viewModelScope.launch {
        accountRepository.reorder(orderedIds)
    }
}
