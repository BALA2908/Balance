package com.balance.budget.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.balance.budget.core.ui.components.parseColor
import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.core.util.Money
import com.balance.budget.domain.ai.AiText
import com.balance.budget.domain.ai.AiTextSource
import com.balance.budget.domain.analytics.AnalyticsSnapshot
import com.balance.budget.domain.analytics.SpendingPersonality
import com.balance.budget.domain.analytics.TrendDirection
import com.balance.budget.feature.reports.charts.CategoryDonut
import com.balance.budget.feature.reports.charts.SpendTrendChart
import kotlin.math.roundToInt

@Composable
fun ReportsScreen(
    contentPadding: PaddingValues,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val s = state.snapshot

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Reports",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 28.dp),
        )
        Text(
            text = DateTimeUtil.monthLabel(s.month),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        if (s.isEmpty && !state.isLoading) {
            EmptyReports()
            return@Column
        }

        // "Month in a sentence" — AI summary with graceful fallback.
        AiCard(title = "Your month in a sentence", body = state.summary)

        Spacer(Modifier.height(16.dp))

        // Spend trend
        SectionTitle("Spending this month")
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Money.formatWhole(s.monthToDateMinor),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (state.dailyCumulative.size >= 2) {
                    SpendTrendChart(
                        cumulative = state.dailyCumulative,
                        budgetMinor = s.overallBudgetMinor,
                        lineColor = MaterialTheme.colorScheme.primary,
                        reduceMotion = state.reduceMotion,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(top = 12.dp),
                    )
                } else {
                    Text(
                        text = "A day or two more and your trend line will appear.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Category donut + legend
        if (s.topCategories.isNotEmpty()) {
            SectionTitle("By category")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryDonut(
                    slices = s.byCategory.filter { it.spentMinor > 0 },
                    reduceMotion = state.reduceMotion,
                    modifier = Modifier.size(132.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = Money.formatWhole(s.monthToDateMinor),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    s.topCategories.forEach { slice ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(parseColor(slice.colorHex)),
                            )
                            Text(
                                text = slice.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                            )
                            Text(
                                text = "${slice.percentOfTotal.roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // vs last month
        ComparisonRow(s)

        Spacer(Modifier.height(16.dp))

        // Spending personality (deterministic archetype)
        SpendingPersonality.from(s)?.let { p ->
            SectionTitle("Your spending personality")
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = p.emoji, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(end = 14.dp))
                    Column {
                        Text(p.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                        Text(p.blurb, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Top merchants
        if (s.topMerchants.isNotEmpty()) {
            SectionTitle("Top places")
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    s.topMerchants.forEach { m ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.merchant, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                                Text(
                                    "${m.count} ${if (m.count == 1) "visit" else "visits"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(Money.format(m.spentMinor), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Gentle suggestions
        SectionTitle("Gentle suggestions")
        AiCard(title = null, body = state.tips, modifier = Modifier.padding(top = 8.dp))

        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun AiCard(title: String?, body: AiText, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (body.source != AiTextSource.DETERMINISTIC) {
                        Text(
                            text = "✨ AI",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = body.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun ComparisonRow(s: AnalyticsSnapshot) {
    val mom = s.monthOverMonth
    val word = when (mom.direction) {
        TrendDirection.UP -> "up"
        TrendDirection.DOWN -> "down"
        TrendDirection.FLAT -> "about the same"
    }
    val detail = mom.percentChange?.let { "$word ${kotlin.math.abs(it).roundToInt()}% vs last month" }
        ?: "vs last month (no prior data)"
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Pace",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
private fun EmptyReports() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("📊", style = MaterialTheme.typography.displayMedium)
        Text(
            "No data yet",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "Add a few expenses and your charts and monthly story will appear here.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
