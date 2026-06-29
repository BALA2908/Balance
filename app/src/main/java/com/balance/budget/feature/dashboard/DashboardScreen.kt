package com.balance.budget.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.balance.budget.core.haptics.rememberHaptics
import com.balance.budget.core.ui.components.BudgetRing
import com.balance.budget.core.ui.components.ExpenseRow
import com.balance.budget.core.ui.components.MeshBackground
import com.balance.budget.core.ui.components.PressScale
import com.balance.budget.core.ui.components.tint
import com.balance.budget.core.ui.theme.HeroAmountStyle
import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.core.util.Money
import com.balance.budget.domain.analytics.AnalyticsSnapshot
import com.balance.budget.domain.model.BudgetState
import kotlin.math.roundToInt

/**
 * The cozy hero surface. Pure content — the app shell ([AppScaffold]) owns the
 * bottom bar, the ＋ FAB, and the Quick Add sheet, and passes [contentPadding].
 */
@Composable
fun DashboardScreen(
    contentPadding: PaddingValues,
    onNavigateToBudgets: () -> Unit,
    onNavigateToAsk: () -> Unit,
    onNavigateToMoneyStory: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snapshot = state.snapshot
    val haptics = rememberHaptics()

    // One gentle "you've crossed your budget" buzz on the transition into OVER.
    var wasOver by remember { mutableStateOf(false) }
    LaunchedEffect(snapshot.overallState) {
        val isOver = snapshot.overallState == BudgetState.OVER
        if (isOver && !wasOver) haptics.reject()
        wasOver = isOver
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MeshBackground(reduceMotion = state.reduceMotion)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = DateTimeUtil.monthLabel(snapshot.month),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 28.dp, bottom = 4.dp),
                )
            }

            item {
                SafeToSpendHero(
                    safeToSpend = snapshot.safeToSpend,
                    monthRemainingMinor = snapshot.overallRemainingMinor,
                    state = snapshot.overallState,
                    onSetBudget = onNavigateToBudgets,
                    reduceMotion = state.reduceMotion,
                    rolloverCarryMinor = snapshot.rolloverCarryMinor,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            if (snapshot.overallBudgetMinor != null) {
                item { BudgetProgressCard(snapshot, state.reduceMotion) }
            }

            item {
                DashboardActions(
                    onAsk = onNavigateToAsk,
                    onStory = onNavigateToMoneyStory,
                    streakDays = snapshot.streaks.currentUnderBudgetDays,
                )
            }

            if (snapshot.topCategories.isNotEmpty()) {
                item { SectionHeader("Where it went", Modifier.padding(top = 16.dp, bottom = 4.dp)) }
                item {
                    CategoryBreakdownStrip(
                        slices = snapshot.topCategories,
                        reduceMotion = state.reduceMotion,
                    )
                }
            }

            if (state.hasExpenses) {
                item { SectionHeader("Recent", Modifier.padding(top = 20.dp, bottom = 4.dp)) }
                items(state.recent, key = { it.expense.id }) { item ->
                    ExpenseRow(item = item)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                }
                item { Spacer(Modifier.height(96.dp)) } // breathing room under the FAB
            } else if (!state.isLoading) {
                item { EmptyRecent(Modifier.padding(top = 48.dp)) }
            }
        }
    }
}

@Composable
private fun DashboardActions(onAsk: () -> Unit, onStory: () -> Unit, streakDays: Int) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ActionCard("💬", "Ask", "Can I afford…?", onAsk, Modifier.weight(1f))
        ActionCard(
            emoji = "📖",
            title = "Your story",
            subtitle = if (streakDays >= 2) "$streakDays-day streak 🔥" else "This week",
            onClick = onStory,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressScale(onClick = onClick, modifier = modifier) { _ ->
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun BudgetProgressCard(snapshot: AnalyticsSnapshot, reduceMotion: Boolean) {
    val budget = snapshot.overallBudgetMinor ?: return
    val fraction = if (budget > 0) (snapshot.monthToDateMinor.toDouble() / budget).toFloat() else 0f
    val percent = (fraction * 100).roundToInt()
    val tint = snapshot.overallState.tint()

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BudgetRing(
                fraction = fraction,
                color = tint,
                reduceMotion = reduceMotion,
                modifier = Modifier.size(84.dp),
            ) {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${Money.formatWhole(snapshot.monthToDateMinor)} spent",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "of ${Money.formatWhole(budget)} budget",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = projectionLine(snapshot),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tint,
                )
            }
        }
    }
}

private fun projectionLine(snapshot: AnalyticsSnapshot): String {
    val projected = snapshot.projection.projectedMonthEndMinor
    val over = snapshot.projection.projectedOverBudgetMinor
    return when {
        snapshot.projection.onTrack == true ->
            "On track · ~${Money.formatWhole(projected)} by month-end"
        over != null && over > 0 ->
            "~${Money.formatWhole(projected)} projected · ${Money.formatWhole(over)} over"
        else -> "~${Money.formatWhole(projected)} projected by month-end"
    }
}

@Composable
private fun EmptyRecent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "🪴", style = HeroAmountStyle)
        Text(
            text = "Nothing here yet",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Tap ＋ to add your first expense.\nIt takes about two seconds.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
