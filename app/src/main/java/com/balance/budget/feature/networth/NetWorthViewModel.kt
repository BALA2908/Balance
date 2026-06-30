package com.balance.budget.feature.networth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.data.repository.AccountRepository
import com.balance.budget.data.repository.BalanceSnapshotRepository
import com.balance.budget.data.repository.ExpenseRepository
import com.balance.budget.domain.model.Account
import com.balance.budget.domain.model.BalanceSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class NetWorthUiState(
    val accounts: List<Account> = emptyList(),
    val netWorthMinor: Long = 0,
    val spentThisMonthByAccount: Map<Long, Long> = emptyMap(),
    val snapshots: List<BalanceSnapshot> = emptyList(),
) {
    /** True once at least one wallet has a balance set. */
    val hasBalances: Boolean get() = accounts.any { it.openingBalanceMinor != null }
}

/**
 * "Wealth view": net worth = sum of wallet balances the user maintains, plus a
 * derived "spent this month" per wallet, plus a manual balance-over-time history.
 * All sums are plain Kotlin — never the AI layer.
 */
@HiltViewModel
class NetWorthViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val snapshotRepository: BalanceSnapshotRepository,
    expenseRepository: ExpenseRepository,
    private val clock: () -> Long,
) : ViewModel() {

    private val month: YearMonth = DateTimeUtil.yearMonth(clock())

    val state: StateFlow<NetWorthUiState> = combine(
        accountRepository.observeActive(),
        snapshotRepository.observeAll(),
        expenseRepository.observeForMonth(month),
    ) { accounts, snapshots, monthExpenses ->
        NetWorthUiState(
            accounts = accounts,
            netWorthMinor = accounts.sumOf { it.openingBalanceMinor ?: 0L },
            spentThisMonthByAccount = monthExpenses
                .filter { it.expense.accountId != null }
                .groupBy { it.expense.accountId!! }
                .mapValues { (_, rows) -> rows.sumOf { it.expense.amountMinor } },
            snapshots = snapshots,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NetWorthUiState())

    /** Save the current total net worth as a dated point for the trend chart. */
    fun recordSnapshot() = viewModelScope.launch {
        snapshotRepository.record(state.value.netWorthMinor)
    }
}
