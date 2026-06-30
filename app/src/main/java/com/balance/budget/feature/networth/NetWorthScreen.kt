package com.balance.budget.feature.networth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.balance.budget.core.ui.components.accountIconForKey
import com.balance.budget.core.ui.components.parseColor
import com.balance.budget.core.util.Money
import com.balance.budget.domain.model.Account
import com.balance.budget.feature.reports.charts.SpendTrendChart

@Composable
fun NetWorthScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: NetWorthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Text(
                text = "Net worth",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            if (state.hasBalances) {
                TextButton(onClick = { viewModel.recordSnapshot() }) { Text("Record") }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Total net worth", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = Money.format(state.netWorthMinor),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        if (state.snapshots.size >= 2) {
                            SpendTrendChart(
                                cumulative = state.snapshots.map { it.netWorthMinor },
                                budgetMinor = null,
                                lineColor = MaterialTheme.colorScheme.primary,
                                reduceMotion = false,
                                modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 12.dp),
                            )
                        } else {
                            Text(
                                text = "Tap Record now and again to chart your net worth over time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }

            if (!state.hasBalances) {
                item {
                    Text(
                        text = "Set a balance on your wallets in Settings → Accounts & wallets, and your net worth will appear here.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp),
                    )
                }
            }

            item {
                Text(
                    text = "Wallets",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }
            items(state.accounts, key = { it.id }) { account ->
                WalletRow(account, state.spentThisMonthByAccount[account.id] ?: 0L)
            }
        }
    }
}

@Composable
private fun WalletRow(account: Account, spentThisMonth: Long) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val color = parseColor(account.colorHex)
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(accountIconForKey(account.iconKey), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    text = if (spentThisMonth > 0) "${Money.format(spentThisMonth)} spent this month" else "no spend this month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = account.openingBalanceMinor?.let { Money.format(it) } ?: "—",
                style = MaterialTheme.typography.titleMedium,
                color = if (account.openingBalanceMinor != null) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
