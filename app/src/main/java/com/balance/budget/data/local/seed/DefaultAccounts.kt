package com.balance.budget.data.local.seed

import com.balance.budget.data.local.entity.AccountEntity
import com.balance.budget.domain.model.AccountType

/**
 * The starter wallets, seeded on first launch. Cash-first: **Cash** is the default
 * and is what Quick Add pre-selects. The user can rename, recolour, reorder, add,
 * or archive any of these. Existing installs get the same set back-filled by the
 * v3→v4 migration (Cash carries the back-filled expenses).
 */
object DefaultAccounts {

    val list: List<AccountEntity> = listOf(
        acc("Cash", AccountType.CASH, "cash", "#8FB996", isDefault = true, order = 0),
        acc("Bank", AccountType.BANK, "bank", "#7E97C9", isDefault = false, order = 1),
        acc("UPI", AccountType.WALLET, "upi", "#F0A868", isDefault = false, order = 2),
        acc("Card", AccountType.CARD, "card", "#B98BC9", isDefault = false, order = 3),
    )

    private fun acc(
        name: String,
        type: AccountType,
        icon: String,
        color: String,
        isDefault: Boolean,
        order: Int,
    ) = AccountEntity(
        name = name,
        type = type,
        iconKey = icon,
        colorHex = color,
        openingBalanceMinor = null,
        isDefault = isDefault,
        isArchived = false,
        sortOrder = order,
    )
}
