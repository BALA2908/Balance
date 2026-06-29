package com.balance.budget.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.balance.budget.core.ui.theme.HeroAmountStyle
import com.balance.budget.core.util.Money
import com.balance.budget.feature.quickadd.NumberPad

/**
 * A reusable bottom sheet for entering a single ₹ amount on the tactile number
 * pad — used for setting overall and per-category budgets (and reusable for any
 * future amount entry). Keeps the number-pad input rules in one place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountEntrySheet(
    title: String,
    onSave: (amountMinor: Long) -> Unit,
    onDismiss: () -> Unit,
    initialMinor: Long? = null,
    saveLabel: String = "Save",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var input by remember {
        mutableStateOf(initialMinor?.takeIf { it > 0 }?.let { Money.formatPlain(it).replace(",", "") } ?: "")
    }
    val amountMinor = Money.parseToMinor(input) ?: 0L

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Money.RUPEE,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = input.ifEmpty { "0" },
                    style = HeroAmountStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            NumberPad(
                onDigit = { d -> input = appendDigit(input, d) },
                onDecimal = { input = appendDecimal(input) },
                onBackspace = { input = input.dropLast(1) },
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Button(
                onClick = { if (amountMinor > 0) onSave(amountMinor) },
                enabled = amountMinor > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(saveLabel, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private const val MAX_INT_DIGITS = 9

private fun appendDigit(current: String, d: Int): String {
    if (d !in 0..9) return current
    if (current == "0" && d == 0) return current
    val next = if (current == "0") d.toString() else current + d.toString()
    val dot = next.indexOf('.')
    val intLen = if (dot >= 0) dot else next.length
    val fracLen = if (dot >= 0) next.length - dot - 1 else 0
    return if (intLen <= MAX_INT_DIGITS && fracLen <= 2) next else current
}

private fun appendDecimal(current: String): String =
    if (current.contains('.')) current else if (current.isEmpty()) "0." else "$current."
