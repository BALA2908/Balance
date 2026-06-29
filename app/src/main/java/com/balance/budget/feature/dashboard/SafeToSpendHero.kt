package com.balance.budget.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.balance.budget.core.ui.components.CountUpAmount
import com.balance.budget.core.ui.components.PressScale
import com.balance.budget.core.ui.components.tint
import com.balance.budget.core.ui.theme.HeroAmountStyle
import com.balance.budget.core.util.Money
import com.balance.budget.domain.analytics.SafeToSpend
import com.balance.budget.domain.analytics.SafeToSpendBasis
import com.balance.budget.domain.model.BudgetState

/**
 * The signature surface: one calm, guilt-free number answering "what can I spend
 * today?". State-tinted, with the month's remaining shown quietly beneath. When
 * no budget exists yet, it becomes a gentle prompt to set one.
 */
@Composable
fun SafeToSpendHero(
    safeToSpend: SafeToSpend,
    monthRemainingMinor: Long?,
    state: BudgetState,
    onSetBudget: () -> Unit,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    rolloverCarryMinor: Long = 0,
) {
    when (safeToSpend.basis) {
        SafeToSpendBasis.NO_BUDGET -> NoBudgetPrompt(onSetBudget, modifier)
        else -> Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "Safe to spend today",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (reduceMotion) {
                Text(
                    text = Money.formatWhole(safeToSpend.perDayMinor),
                    style = HeroAmountStyle,
                    color = state.tint(),
                )
            } else {
                CountUpAmount(
                    amountMinor = safeToSpend.perDayMinor,
                    style = HeroAmountStyle,
                    color = state.tint(),
                    whole = true,
                )
            }
            val sub = when (safeToSpend.basis) {
                SafeToSpendBasis.EXHAUSTED ->
                    "You're over budget this month — take it easy 🌙"
                else -> monthRemainingMinor?.let { "${Money.formatWhole(it)} left this month" }
                    ?: "${Money.formatWhole(safeToSpend.remainingPoolMinor)} left this month"
            }
            Text(
                text = sub,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (rolloverCarryMinor > 0) {
                Text(
                    text = "🌱 includes +${Money.formatWhole(rolloverCarryMinor)} rolled over",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun NoBudgetPrompt(onSetBudget: () -> Unit, modifier: Modifier) {
    PressScale(onClick = onSetBudget, modifier = modifier.fillMaxWidth()) { pressed ->
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Set a monthly budget",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "So Balance can show your safe-to-spend each day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
