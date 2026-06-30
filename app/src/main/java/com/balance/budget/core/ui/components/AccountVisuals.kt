package com.balance.budget.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.ui.graphics.vector.ImageVector

/** Maps an account's stable iconKey to a Material icon. Unknown keys → a wallet. */
fun accountIconForKey(iconKey: String): ImageVector = when (iconKey) {
    "cash" -> Icons.Outlined.Payments
    "bank" -> Icons.Outlined.AccountBalance
    "upi" -> Icons.Outlined.QrCode2
    "card" -> Icons.Outlined.CreditCard
    "wallet" -> Icons.Outlined.AccountBalanceWallet
    "savings" -> Icons.Outlined.Savings
    else -> Icons.Outlined.Wallet
}

/** Icon keys offered in the account-manager icon picker, in display order. */
val AccountIconCatalog: List<String> = listOf(
    "cash", "bank", "upi", "card", "wallet", "savings",
)
