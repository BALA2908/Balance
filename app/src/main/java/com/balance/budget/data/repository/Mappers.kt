package com.balance.budget.data.repository

import com.balance.budget.data.local.dao.ExpenseWithCategoryRow
import com.balance.budget.data.local.entity.AccountEntity
import com.balance.budget.data.local.entity.BudgetAdjustmentEntity
import com.balance.budget.data.local.entity.BudgetEntity
import com.balance.budget.data.local.entity.CategoryEntity
import com.balance.budget.data.local.entity.CategoryRuleEntity
import com.balance.budget.data.local.entity.ExpenseEntity
import com.balance.budget.data.local.entity.ImportCandidateEntity
import com.balance.budget.data.local.entity.RecurringEntity
import com.balance.budget.data.local.entity.SavingsGoalEntity
import com.balance.budget.data.local.entity.TagEntity
import com.balance.budget.domain.analytics.BudgetAdjustment
import com.balance.budget.domain.model.Account
import com.balance.budget.domain.model.Budget
import com.balance.budget.domain.model.ImportCandidate
import com.balance.budget.domain.model.Category
import com.balance.budget.domain.model.Expense
import com.balance.budget.domain.model.ExpenseWithCategory
import com.balance.budget.domain.model.CategoryRule
import com.balance.budget.domain.model.Recurring
import com.balance.budget.domain.model.SavingsGoal
import com.balance.budget.domain.model.Tag

/** Entity → domain and back. Kept in one place so the boundary is obvious. */

fun CategoryEntity.toDomain() = Category(
    id = id, name = name, iconKey = iconKey, colorHex = colorHex,
    isDefault = isDefault, isArchived = isArchived, sortOrder = sortOrder,
)

fun Category.toEntity() = CategoryEntity(
    id = id, name = name, iconKey = iconKey, colorHex = colorHex,
    isDefault = isDefault, isArchived = isArchived, sortOrder = sortOrder,
)

fun ExpenseEntity.toDomain() = Expense(
    id = id, amountMinor = amountMinor, categoryId = categoryId, note = note,
    timestamp = timestamp, createdAt = createdAt, source = source, merchant = merchant,
    accountId = accountId,
)

fun AccountEntity.toDomain() = Account(
    id = id, name = name, type = type, iconKey = iconKey, colorHex = colorHex,
    openingBalanceMinor = openingBalanceMinor, isDefault = isDefault,
    isArchived = isArchived, sortOrder = sortOrder,
)

fun Account.toEntity() = AccountEntity(
    id = id, name = name, type = type, iconKey = iconKey, colorHex = colorHex,
    openingBalanceMinor = openingBalanceMinor, isDefault = isDefault,
    isArchived = isArchived, sortOrder = sortOrder,
)

fun TagEntity.toDomain() = Tag(id = id, name = name, colorHex = colorHex, sortOrder = sortOrder)

fun Tag.toEntity() = TagEntity(id = id, name = name, colorHex = colorHex, sortOrder = sortOrder)

fun CategoryRuleEntity.toDomain() = CategoryRule(id = id, pattern = pattern, categoryId = categoryId, sortOrder = sortOrder)

fun CategoryRule.toEntity() = CategoryRuleEntity(id = id, pattern = pattern, categoryId = categoryId, sortOrder = sortOrder)

fun SavingsGoalEntity.toDomain() = SavingsGoal(
    id = id, name = name, iconKey = iconKey, colorHex = colorHex,
    targetMinor = targetMinor, savedMinor = savedMinor, sortOrder = sortOrder,
)

fun SavingsGoal.toEntity() = SavingsGoalEntity(
    id = id, name = name, iconKey = iconKey, colorHex = colorHex,
    targetMinor = targetMinor, savedMinor = savedMinor, sortOrder = sortOrder,
)

fun ExpenseWithCategoryRow.toDomain() = ExpenseWithCategory(
    expense = expense.toDomain(),
    category = category.toDomain(),
)

fun BudgetEntity.toDomain() = Budget(
    id = id, categoryId = categoryId, amountMinor = amountMinor,
    period = period, effectiveFromYearMonth = effectiveFromYearMonth,
)

fun RecurringEntity.toDomain() = Recurring(
    id = id, amountMinor = amountMinor, categoryId = categoryId, note = note,
    cadence = cadence, anchorDay = anchorDay, nextDueDate = nextDueDate, isActive = isActive,
)

fun Recurring.toEntity() = RecurringEntity(
    id = id, amountMinor = amountMinor, categoryId = categoryId, note = note,
    cadence = cadence, anchorDay = anchorDay, nextDueDate = nextDueDate, isActive = isActive,
)

fun ImportCandidateEntity.toDomain() = ImportCandidate(
    id = id, amountMinor = amountMinor, merchant = merchant, rawText = rawText,
    sourceApp = sourceApp, postedAt = postedAt, suggestedCategoryId = suggestedCategoryId,
)

fun BudgetAdjustmentEntity.toDomain() = BudgetAdjustment(
    fromCategoryId = fromCategoryId, toCategoryId = toCategoryId, amountMinor = amountMinor,
)
