package com.balance.budget.feature.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.balance.budget.core.ui.components.PressScale
import com.balance.budget.core.ui.components.iconForKey
import com.balance.budget.core.ui.components.parseColor
import com.balance.budget.core.util.Money
import com.balance.budget.domain.model.Category
import com.balance.budget.domain.model.Recurring
import com.balance.budget.domain.model.RecurringCadence
import com.balance.budget.feature.quickadd.CategoryChipRow

private val DOW = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun cadenceLabel(cadence: RecurringCadence, anchorDay: Int): String = when (cadence) {
    RecurringCadence.MONTHLY -> "Monthly · day ${anchorDay.coerceIn(1, 28)}"
    RecurringCadence.WEEKLY -> "Weekly · ${DOW.getOrElse(anchorDay.coerceIn(1, 7) - 1) { "Mon" }}"
}

@Composable
fun RecurringScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: RecurringViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var editing: Recurring? by remember { mutableStateOf(null) }
    var showAdd by remember { mutableStateOf(false) }
    val categoriesById = state.categories.associateBy { it.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Recurring",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                )
                TextButton(onClick = { showAdd = true }) { Text("Add") }
            }
        }

        if (state.items.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("🔁", style = MaterialTheme.typography.displayMedium)
                    Text(
                        "No recurring expenses yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "Add rent, subscriptions, or bills — they're reserved from your safe-to-spend.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(state.items, key = { it.id }) { item ->
                RecurringRow(
                    recurring = item,
                    category = categoriesById[item.categoryId],
                    onClick = { editing = item },
                    onToggleActive = { viewModel.setActive(item, it) },
                )
            }
        }
    }

    if (showAdd) {
        RecurringEditorSheet(
            initial = null,
            categories = state.categories,
            onSave = { amount, catId, note, cadence, day ->
                viewModel.add(amount, catId, note, cadence, day); showAdd = false
            },
            onDelete = null,
            onDismiss = { showAdd = false },
        )
    }
    editing?.let { current ->
        RecurringEditorSheet(
            initial = current,
            categories = state.categories,
            onSave = { amount, catId, note, cadence, day ->
                viewModel.update(
                    current.copy(
                        amountMinor = amount, categoryId = catId, note = note,
                        cadence = cadence, anchorDay = day,
                    )
                ); editing = null
            },
            onDelete = { viewModel.delete(current); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun RecurringRow(
    recurring: Recurring,
    category: Category?,
    onClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
) {
    PressScale(onClick = onClick, modifier = Modifier.fillMaxWidth()) { _ ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val color = parseColor(category?.colorHex ?: "#9C8C7A")
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconForKey(category?.iconKey ?: "other"),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = recurring.note?.takeIf { it.isNotBlank() } ?: category?.name ?: "Recurring",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${Money.format(recurring.amountMinor)} · ${cadenceLabel(recurring.cadence, recurring.anchorDay)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = recurring.isActive, onCheckedChange = onToggleActive)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringEditorSheet(
    initial: Recurring?,
    categories: List<Category>,
    onSave: (amountMinor: Long, categoryId: Long, note: String?, cadence: RecurringCadence, anchorDay: Int) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember {
        mutableStateOf(initial?.let { Money.formatPlain(it.amountMinor).replace(",", "") } ?: "")
    }
    var selectedCategoryId by remember { mutableStateOf(initial?.categoryId) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var cadence by remember { mutableStateOf(initial?.cadence ?: RecurringCadence.MONTHLY) }
    var monthlyDay by remember {
        mutableStateOf(
            if (initial != null && initial.cadence == RecurringCadence.MONTHLY)
                initial.anchorDay.coerceIn(1, 28) else 1
        )
    }
    var weeklyDay by remember {
        mutableStateOf(
            if (initial != null && initial.cadence == RecurringCadence.WEEKLY)
                initial.anchorDay.coerceIn(1, 7) else 1
        )
    }

    val amountMinor = Money.parseToMinor(amountText) ?: 0L
    val canSave = amountMinor > 0 && selectedCategoryId != null

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (initial == null) "New recurring expense" else "Edit recurring expense",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { new -> amountText = new.filter { it.isDigit() || it == '.' } },
                label = { Text("Amount (₹)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Category", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            CategoryChipRow(
                categories = categories,
                selectedId = selectedCategoryId,
                onSelect = { selectedCategoryId = it },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (e.g. Rent, Netflix)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Repeats", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = cadence == RecurringCadence.MONTHLY,
                    onClick = { cadence = RecurringCadence.MONTHLY },
                    label = { Text("Monthly") },
                )
                FilterChip(
                    selected = cadence == RecurringCadence.WEEKLY,
                    onClick = { cadence = RecurringCadence.WEEKLY },
                    label = { Text("Weekly") },
                )
            }
            if (cadence == RecurringCadence.MONTHLY) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Day of month", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = { if (monthlyDay > 1) monthlyDay-- }) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Earlier day")
                    }
                    Text("$monthlyDay", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { if (monthlyDay < 28) monthlyDay++ }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Later day")
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DOW.forEachIndexed { index, label ->
                        FilterChip(
                            selected = weeklyDay == index + 1,
                            onClick = { weeklyDay = index + 1 },
                            label = { Text(label) },
                        )
                    }
                }
            }
            Button(
                onClick = {
                    val day = if (cadence == RecurringCadence.MONTHLY) monthlyDay else weeklyDay
                    onSave(amountMinor, selectedCategoryId!!, note, cadence, day)
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save", style = MaterialTheme.typography.titleMedium) }

            if (onDelete != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
