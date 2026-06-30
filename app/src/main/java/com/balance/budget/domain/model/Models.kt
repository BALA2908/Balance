package com.balance.budget.domain.model

/**
 * Domain models — what the rest of the app (ViewModels, analytics, UI) works
 * with. They're deliberately decoupled from the Room entities so the storage
 * shape can change without rippling through the UI. Mapping lives in the
 * repositories.
 */

data class Category(
    val id: Long,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val isDefault: Boolean,
    val isArchived: Boolean,
    val sortOrder: Int,
)

data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val iconKey: String,
    val colorHex: String,
    val openingBalanceMinor: Long?,
    val isDefault: Boolean,
    val isArchived: Boolean,
    val sortOrder: Int,
)

data class Tag(
    val id: Long,
    val name: String,
    val colorHex: String,
    val sortOrder: Int,
)

data class CategoryRule(
    val id: Long,
    val pattern: String,
    val categoryId: Long,
    val sortOrder: Int,
)

data class SavingsGoal(
    val id: Long,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val targetMinor: Long,
    val savedMinor: Long,
    val sortOrder: Int,
) {
    val fraction: Float get() = if (targetMinor > 0) (savedMinor.toFloat() / targetMinor).coerceIn(0f, 1f) else 0f
    val isComplete: Boolean get() = targetMinor > 0 && savedMinor >= targetMinor
}

data class Expense(
    val id: Long,
    val amountMinor: Long,
    val categoryId: Long,
    val note: String?,
    val timestamp: Long,
    val createdAt: Long,
    val source: ExpenseSource,
    val merchant: String?,
    /** Wallet/payment method this was paid from; null = unassigned. */
    val accountId: Long? = null,
)

/** An expense joined with its category, for display in lists. */
data class ExpenseWithCategory(
    val expense: Expense,
    val category: Category,
)

data class Budget(
    val id: Long,
    val categoryId: Long?, // null = overall monthly budget
    val amountMinor: Long,
    val period: BudgetPeriod,
    val effectiveFromYearMonth: Int,
)

data class Recurring(
    val id: Long,
    val amountMinor: Long,
    val categoryId: Long,
    val note: String?,
    val cadence: RecurringCadence,
    val anchorDay: Int,
    val nextDueDate: Long,
    val isActive: Boolean,
)

/**
 * A parsed-but-unconfirmed transaction from a UPI/bank notification, awaiting
 * the user's review (Phase 4 auto-import). Never a real expense until confirmed.
 */
data class ImportCandidate(
    val id: Long,
    val amountMinor: Long,
    val merchant: String?,
    val rawText: String,
    val sourceApp: String,
    val postedAt: Long,
    val suggestedCategoryId: Long?,
)

/**
 * Everything needed to create a new expense from the Quick Add screen.
 * One draft type → one save path, shared by the FAB and the deep-link entry.
 */
data class ExpenseDraft(
    val amountMinor: Long,
    val categoryId: Long,
    val note: String?,
    val timestamp: Long,
    val source: ExpenseSource = ExpenseSource.MANUAL,
    val merchant: String? = null,
    /** Wallet/payment method; null = unassigned (resolved to default by Quick Add). */
    val accountId: Long? = null,
    /** Tags to attach after the expense row is written. */
    val tagIds: List<Long> = emptyList(),
)
