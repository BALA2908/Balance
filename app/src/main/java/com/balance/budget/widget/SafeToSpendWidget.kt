package com.balance.budget.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.balance.budget.MainActivity
import com.balance.budget.QuickAddActivity
import com.balance.budget.core.util.Money
import com.balance.budget.domain.analytics.AnalyticsSnapshot
import com.balance.budget.domain.analytics.SafeToSpendBasis
import com.balance.budget.domain.model.BudgetState
import dagger.hilt.android.EntryPointAccessors

/**
 * Home-screen widget: today's safe-to-spend, a budget-used progress bar tinted by
 * status, and a one-tap Add. Tapping the card opens the app; the pill opens Quick
 * Add. Reads a one-shot snapshot via a Hilt entry point; responsive to the placed
 * size (compact = amount + add; taller = + progress + spent line).
 */
class SafeToSpendWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val snapshot = runCatching { entryPoint.analyticsRepository().snapshotOnce() }.getOrNull()
        provideContent { WidgetBody(context, snapshot) }
    }

    @Composable
    private fun WidgetBody(context: Context, snapshot: AnalyticsSnapshot?) {
        val sts = snapshot?.safeToSpend
        val hasBudget = sts != null && sts.basis != SafeToSpendBasis.NO_BUDGET
        val compact = LocalSize.current.height < 116.dp

        val heroColor = when {
            !hasBudget -> CREAM
            snapshot?.overallState == BudgetState.OVER -> CLAY
            snapshot?.overallState == BudgetState.APPROACHING -> HONEY
            else -> SAGE
        }
        val budget = snapshot?.overallBudgetMinor
        val spent = snapshot?.monthToDateMinor ?: 0L
        val progress = if (budget != null && budget > 0) (spent.toFloat() / budget).coerceIn(0f, 1f) else 0f
        val streak = snapshot?.streaks?.currentUnderBudgetDays ?: 0

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(ESPRESSO))
                .cornerRadius(24.dp)
                .clickable(actionStartActivity(appIntent(context)))
                .padding(16.dp)
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Safe to spend today",
                    style = TextStyle(color = ColorProvider(LATTE), fontSize = 12.sp),
                    modifier = GlanceModifier.defaultWeight(),
                )
                if (streak >= 2) {
                    Text("🔥 $streak", style = TextStyle(color = ColorProvider(HONEY), fontSize = 12.sp, fontWeight = FontWeight.Medium))
                }
            }
            Text(
                text = if (hasBudget) Money.formatWhole(sts!!.perDayMinor) else "Set a budget",
                style = TextStyle(
                    color = ColorProvider(heroColor),
                    fontSize = if (compact) 26.sp else 30.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            if (!compact && hasBudget && budget != null) {
                Spacer(GlanceModifier.height(10.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(3.dp),
                    color = ColorProvider(heroColor),
                    backgroundColor = ColorProvider(SAND),
                )
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    text = "${Money.formatWhole(spent)} of ${Money.formatWhole(budget)}",
                    style = TextStyle(color = ColorProvider(LATTE), fontSize = 12.sp),
                )
            }
            Spacer(GlanceModifier.height(if (compact) 10.dp else 12.dp))
            Text(
                text = "＋ Add expense",
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity(addExpenseIntent(context)))
                    .background(ColorProvider(AMBER))
                    .cornerRadius(20.dp)
                    .padding(vertical = 9.dp),
                style = TextStyle(
                    color = ColorProvider(ESPRESSO),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }

    private fun addExpenseIntent(context: Context): Intent =
        Intent(context, QuickAddActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun appIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private companion object {
        val ESPRESSO = Color(0xFF16130F)
        val SAND = Color(0xFF3A332A)
        val AMBER = Color(0xFFF0A868)
        val CREAM = Color(0xFFF5EDE2)
        val LATTE = Color(0xFFC9BCA9)
        val SAGE = Color(0xFF8FB996)
        val HONEY = Color(0xFFE8C06B)
        val CLAY = Color(0xFFE08B7B)
    }
}

class SafeToSpendWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SafeToSpendWidget()
}
