package com.balance.budget.data.repository

import com.balance.budget.domain.model.ExpenseDraft
import com.balance.budget.domain.model.ExpenseSource
import com.balance.budget.domain.recurring.RecurringMaterializer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs [RecurringMaterializer] over the active recurring items and persists the
 * results: inserts due expenses (source = RECURRING) through the single save path
 * and advances each item's next-due-date. Invoked on app open; idempotent.
 */
@Singleton
class RecurringMaterializerRunner @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val expenseRepository: ExpenseRepository,
    private val clock: () -> Long,
) {
    suspend fun run() {
        val now = clock()
        recurringRepository.activeOnce().forEach { recurring ->
            val result = RecurringMaterializer.materialize(recurring, now)
            if (result.expenses.isNotEmpty()) {
                result.expenses.forEach { e ->
                    expenseRepository.addExpense(
                        ExpenseDraft(
                            amountMinor = e.amountMinor,
                            categoryId = e.categoryId,
                            note = e.note,
                            timestamp = e.timestamp,
                            source = ExpenseSource.RECURRING,
                            merchant = null,
                        )
                    )
                }
                recurringRepository.updateNextDueDate(recurring.id, result.newNextDueDate)
            }
        }
    }
}
