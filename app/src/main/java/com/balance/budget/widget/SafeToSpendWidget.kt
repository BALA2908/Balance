package com.balance.budget.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.balance.budget.QuickAddActivity
import com.balance.budget.core.util.Money
import com.balance.budget.domain.analytics.AnalyticsSnapshot
import com.balance.budget.domain.analytics.SafeToSpendBasis
import dagger.hilt.android.EntryPointAccessors

/**
 * Home-screen widget: today's safe-to-spend at a glance + one-tap add (launches
 * the Quick Add popup via [QuickAddActivity]). Reads a one-shot snapshot through
 * a Hilt entry point. Refreshes on its update period and when re-placed.
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

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF16130F)))
                .cornerRadius(24.dp)
                .padding(16.dp)
        ) {
            Text(
                text = "Safe to spend today",
                style = TextStyle(color = ColorProvider(Color(0xFFC9BCA9)), fontSize = 12.sp),
            )
            Text(
                text = if (hasBudget) Money.formatWhole(sts!!.perDayMinor) else "Set a budget",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFF5EDE2)),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(10.dp))
            Text(
                text = "＋ Add expense",
                modifier = GlanceModifier
                    .clickable(actionStartActivity(addExpenseIntent(context)))
                    .background(ColorProvider(Color(0xFFF0A868)))
                    .cornerRadius(20.dp)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                style = TextStyle(color = ColorProvider(Color(0xFF16130F)), fontWeight = FontWeight.Bold),
            )
        }
    }

    private fun addExpenseIntent(context: Context): Intent =
        Intent(context, QuickAddActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

class SafeToSpendWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SafeToSpendWidget()
}
