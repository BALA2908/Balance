package com.balance.budget.feature.bills

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.balance.budget.core.ui.components.iconForKey
import com.balance.budget.core.ui.components.parseColor
import com.balance.budget.core.util.Money
import com.balance.budget.domain.model.Category
import com.balance.budget.domain.model.RecurringCadence
import com.balance.budget.domain.recurring.UpcomingBill

@Composable
fun BillsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: BillsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val summary = state.summary

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Text(
                text = "Bills & subscriptions",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Text(
                    text = Money.format(summary.monthlyTotalMinor),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "per month across ${summary.activeCount} ${if (summary.activeCount == 1) "subscription" else "subscriptions"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (summary.upcoming.isEmpty()) {
            Text(
                text = "No active recurring bills. Add them under Settings → Recurring expenses.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, start = 4.dp),
            )
            return
        }

        val overdue = summary.upcoming.filter { it.daysUntil < 0 }
        val soon = summary.upcoming.filter { it.daysUntil in 0..7 }
        val thisMonth = summary.upcoming.filter { it.daysUntil in 8..31 }
        val later = summary.upcoming.filter { it.daysUntil > 31 }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            section("Overdue", overdue, state.categoriesById)
            section("Due soon", soon, state.categoriesById)
            section("This month", thisMonth, state.categoriesById)
            section("Later", later, state.categoriesById)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    bills: List<UpcomingBill>,
    categoriesById: Map<Long, Category>,
) {
    if (bills.isEmpty()) return
    item(key = "h-$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
        )
    }
    items(bills.size, key = { "$title-${bills[it].recurring.id}" }) { i ->
        BillRow(bills[i], categoriesById[bills[i].recurring.categoryId])
    }
}

@Composable
private fun BillRow(bill: UpcomingBill, category: Category?) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val color = parseColor(category?.colorHex ?: "#9C8C7A")
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(iconForKey(category?.iconKey ?: "other"), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = bill.recurring.note?.takeIf { it.isNotBlank() } ?: category?.name ?: "Bill",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = dueLabel(bill.daysUntil) + " · " + cadenceLabel(bill.recurring.cadence),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (bill.daysUntil < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = Money.format(bill.recurring.amountMinor),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

private fun dueLabel(daysUntil: Int): String = when {
    daysUntil < -1 -> "overdue ${-daysUntil} days"
    daysUntil == -1 -> "overdue 1 day"
    daysUntil == 0 -> "due today"
    daysUntil == 1 -> "due tomorrow"
    else -> "in $daysUntil days"
}

private fun cadenceLabel(c: RecurringCadence): String = when (c) {
    RecurringCadence.WEEKLY -> "weekly"
    RecurringCadence.MONTHLY -> "monthly"
}
