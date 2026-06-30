package com.balance.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.balance.budget.domain.model.AccountType

/**
 * A wallet / payment method an expense can be paid from (Cash, Bank, UPI, Card…).
 * Cash-first: one account is the default and is what Quick Add pre-selects. Like
 * categories, accounts are archived (soft-deleted) rather than hard-deleted so an
 * expense's [ExpenseEntity.accountId] never dangles. [openingBalanceMinor] is
 * optional and reserved for the Wave-8 net-worth view.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    @ColumnInfo(name = "icon_key") val iconKey: String,
    @ColumnInfo(name = "color_hex") val colorHex: String,
    @ColumnInfo(name = "opening_balance_minor") val openingBalanceMinor: Long? = null,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
)
