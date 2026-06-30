package com.balance.budget.feature.quickadd

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.balance.budget.R
import com.balance.budget.core.ui.components.GlassPanel
import com.balance.budget.core.ui.theme.HeroAmountStyle
import com.balance.budget.core.ui.theme.Motion
import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.core.util.Money

/**
 * The crystal-glass Quick Add body — two frosted panels over the popup's blurred,
 * transparent backdrop: a top entry card (amount + Today chip + categories + note)
 * and a compact frosted keypad below. Shared by the in-app FAB sheet and the
 * deep-link/double-tap [com.balance.budget.QuickAddActivity]; same single save path.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheetContent(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickAddViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    // Fresh entrance: the top card settles in with a soft spring + fade,
    // layered above the keypad which rides the sheet's own slide-up.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val topScale by animateFloatAsState(if (visible) 1f else 0.94f, Motion.sheetSpring(), label = "qa-top-scale")
    val topAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(220), label = "qa-top-alpha")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Top entry card ─────────────────────────────────────────────
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = topScale; scaleY = topScale; alpha = topAlpha },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.quick_add_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AmountDisplay(amountInput = state.amountInput)
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(DateTimeUtil.friendlyDate(state.timestamp)) },
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                )
                CategoryChipRow(
                    categories = state.categories,
                    selectedId = state.selectedCategoryId,
                    onSelect = viewModel::selectCategory,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.accounts.size > 1) {
                    AccountChipRow(
                        accounts = state.accounts,
                        selectedId = state.selectedAccountId,
                        onSelect = viewModel::selectAccount,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (state.tags.isNotEmpty()) {
                    TagChipRow(
                        tags = state.tags,
                        selectedIds = state.selectedTagIds,
                        onToggle = viewModel::toggleTag,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::setNote,
                    placeholder = { Text(stringResource(R.string.quick_add_note_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // ── Compact frosted keypad card ────────────────────────────────
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NumberPad(
                    onDigit = viewModel::onDigit,
                    onDecimal = viewModel::onDecimal,
                    onBackspace = viewModel::onBackspace,
                    compact = true,
                )
                SaveButton(
                    enabled = state.canSave,
                    isSaving = state.isSaving,
                    saved = state.saved,
                    onClick = viewModel::save,
                    onSavedAnimationEnd = onClose,
                )
                state.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let(viewModel::setTimestamp)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun AmountDisplay(amountInput: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = Money.RUPEE,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = amountInput.ifEmpty { "0" },
            style = HeroAmountStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
