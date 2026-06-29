package com.balance.budget.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.balance.budget.core.ui.components.PressScale
import com.balance.budget.domain.model.ThemeMode

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onNavigateToBudgets: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToImportReview: () -> Unit,
    onNavigateToCategories: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Fire the system share sheet when an export file is ready.
    LaunchedEffect(Unit) {
        viewModel.exports.collect { export ->
            val send = Intent(Intent.ACTION_SEND).apply {
                type = export.mime
                putExtra(Intent.EXTRA_STREAM, export.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(send, "Export").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    LaunchedEffect(Unit) {
        viewModel.exportError.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 28.dp, bottom = 12.dp),
        )

        SectionLabel("Budget")
        NavRow("Budgets & limits", "Set your monthly and per-category budgets", onNavigateToBudgets)
        NavRow("Manage categories", "Add, rename, recolour, reorder or archive", onNavigateToCategories)
        ToggleRow(
            title = "Roll over unspent budget",
            subtitle = "Unspent category budget carries into next month and feeds safe-to-spend",
            checked = state.rolloverEnabled,
            onChange = viewModel::setRolloverEnabled,
        )
        NavRow("Recurring expenses", "Rent, subscriptions, bills — reserved from safe-to-spend", onNavigateToRecurring)

        SectionLabel("Appearance")
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                    label = { Text(mode.label()) },
                )
            }
        }
        ToggleRow("Reduce motion", "Calm the ambient background and animations", state.reduceMotion, viewModel::setReduceMotion)

        SectionLabel("AI insights")
        ToggleRow(
            title = "On-device AI",
            subtitle = "Use Gemini Nano on this phone when available (private, free)",
            checked = state.aiOnDevice,
            onChange = viewModel::setAiOnDevice,
        )
        ToggleRow(
            title = "Cloud AI fallback",
            subtitle = "Off by default. Only ever sends anonymized totals, never your transactions.",
            checked = state.aiCloud,
            onChange = viewModel::setAiCloud,
        )

        SectionLabel("Capture")
        ToggleRow(
            title = "Auto-import from notifications",
            subtitle = "Read UPI payment notifications (GPay/PhonePe/Paytm) to pre-fill expenses. Notifications only — never SMS.",
            checked = state.autoImport,
            onChange = viewModel::setAutoImport,
        )
        NavRow(
            "Grant notification access",
            "Required for auto-import — opens system settings",
        ) {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        NavRow(
            "Review imported payments",
            "Confirm parsed UPI transactions before they're added",
            onNavigateToImportReview,
        )

        SectionLabel("Data")
        NavRow("Export this month (CSV)", "Open in a spreadsheet") { viewModel.exportCsv() }
        NavRow("Export this month (PDF)", "A shareable month report") { viewModel.exportPdf() }

        SectionLabel("About")
        Text(
            text = "Currency: ₹ (INR) · Balance 0.1.0",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.DARK -> "Dark"
    ThemeMode.LIGHT -> "Light"
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    PressScale(onClick = onClick, modifier = Modifier.fillMaxWidth()) { _ ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.padding(start = 12.dp))
    }
}
