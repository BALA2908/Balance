package com.balance.budget.feature.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.balance.budget.core.ui.components.AmountEntrySheet
import com.balance.budget.core.ui.components.PressScale
import com.balance.budget.core.ui.components.iconForKey
import com.balance.budget.core.ui.components.parseColor
import com.balance.budget.core.ui.theme.HeroAmountStyle
import com.balance.budget.core.util.Money
import com.balance.budget.domain.model.Category
import com.balance.budget.feature.quickadd.NumberPad

private sealed interface Editing {
    data object Overall : Editing
    data class PerCategory(val category: Category) : Editing
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onManageCategories: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var editing: Editing? by remember { mutableStateOf(null) }
    var showMove by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Budgets",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        if (state.rolloverEnabled && state.rolloverCarryMinor > 0) {
            item { RolloverBanner(state.rolloverCarryMinor) }
        }
        item {
            BudgetRow(
                title = "Monthly budget",
                subtitle = "Your overall spend target",
                valueText = state.overallBudgetMinor?.let { Money.formatWhole(it) } ?: "Set",
                leading = null,
                onClick = { editing = Editing.Overall },
            )
        }
        item {
            Text(
                text = "Per-category limits (optional)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
            )
        }
        items(state.categories, key = { it.id }) { cat ->
            BudgetRow(
                title = cat.name,
                subtitle = carrySubtitle(state.slices[cat.id]),
                valueText = state.categoryBudgets[cat.id]?.let { Money.formatWhole(it) } ?: "Set",
                leading = cat,
                onClick = { editing = Editing.PerCategory(cat) },
            )
        }
        if (state.categoryBudgets.size >= 2) {
            item {
                BudgetRow(
                    title = "Move budget",
                    subtitle = "Shift budget between categories — roll with the punches",
                    valueText = "",
                    leading = null,
                    onClick = { showMove = true },
                )
            }
        }
        item {
            BudgetRow(
                title = "Manage categories",
                subtitle = "Add, rename, recolour, reorder or archive",
                valueText = "",
                leading = null,
                onClick = onManageCategories,
            )
        }
    }

    when (val e = editing) {
        Editing.Overall -> AmountEntrySheet(
            title = "Monthly budget",
            initialMinor = state.overallBudgetMinor,
            onSave = { viewModel.setOverallBudget(it); editing = null },
            onDismiss = { editing = null },
        )
        is Editing.PerCategory -> AmountEntrySheet(
            title = "${e.category.name} budget",
            initialMinor = state.categoryBudgets[e.category.id],
            onSave = { viewModel.setCategoryBudget(e.category.id, it); editing = null },
            onDismiss = { editing = null },
        )
        null -> Unit
    }

    if (showMove) {
        MoveBudgetSheet(
            categories = state.categories.filter { state.categoryBudgets.containsKey(it.id) },
            onMove = { from, to, amount ->
                viewModel.moveBudget(from, to, amount)
                showMove = false
            },
            onDismiss = { showMove = false },
        )
    }
}

/** "+₹X rolled over · moved ±₹Y" for a category row, or null when nothing to show. */
private fun carrySubtitle(slice: com.balance.budget.domain.analytics.CategorySlice?): String? {
    if (slice == null) return null
    val parts = buildList {
        if (slice.carryInMinor > 0) add("+${Money.formatWhole(slice.carryInMinor)} rolled over 🌱")
        if (slice.adjustmentMinor != 0L) {
            val sign = if (slice.adjustmentMinor > 0) "+" else "−"
            add("moved $sign${Money.formatWhole(kotlin.math.abs(slice.adjustmentMinor))}")
        }
    }
    return parts.joinToString(" · ").ifEmpty { null }
}

@Composable
private fun RolloverBanner(carryMinor: Long) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🌱",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column {
                Text(
                    text = "+${Money.formatWhole(carryMinor)} rolled over",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Last month's leftover is added to this month's safe-to-spend",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BudgetRow(
    title: String,
    subtitle: String?,
    valueText: String,
    leading: Category?,
    onClick: () -> Unit,
) {
    PressScale(onClick = onClick, modifier = Modifier.fillMaxWidth()) { _ ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                val color = parseColor(leading.colorHex)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconForKey(leading.iconKey),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (leading != null) 12.dp else 0.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                color = if (valueText == "Set") MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MoveBudgetSheet(
    categories: List<Category>,
    onMove: (fromId: Long, toId: Long, amountMinor: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var fromId by remember { mutableStateOf<Long?>(null) }
    var toId by remember { mutableStateOf<Long?>(null) }
    var input by remember { mutableStateOf("") }
    val amountMinor = Money.parseToMinor(input) ?: 0L
    val canMove = fromId != null && toId != null && fromId != toId && amountMinor > 0L

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = "Move budget",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
            )
            Text(
                text = "Reallocate this month — no shame, just rebalancing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            CategoryPickerLabel("From")
            CategoryChips(categories, selectedId = fromId, onSelect = { fromId = it })
            CategoryPickerLabel("To")
            CategoryChips(categories, selectedId = toId, onSelect = { toId = it })

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Money.RUPEE,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = input.ifEmpty { "0" },
                    style = HeroAmountStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            NumberPad(
                onDigit = { d -> input = appendMoveDigit(input, d) },
                onDecimal = { input = if (input.contains('.')) input else if (input.isEmpty()) "0." else "$input." },
                onBackspace = { input = input.dropLast(1) },
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Button(
                onClick = { if (canMove) onMove(fromId!!, toId!!, amountMinor) },
                enabled = canMove,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("  Move", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun CategoryPickerLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { cat ->
            FilterChip(
                selected = cat.id == selectedId,
                onClick = { onSelect(cat.id) },
                label = { Text(cat.name) },
                leadingIcon = {
                    Icon(
                        imageVector = iconForKey(cat.iconKey),
                        contentDescription = null,
                        tint = parseColor(cat.colorHex),
                        modifier = Modifier.size(18.dp),
                    )
                },
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

private const val MOVE_MAX_INT_DIGITS = 9

private fun appendMoveDigit(current: String, d: Int): String {
    if (d !in 0..9) return current
    if (current == "0" && d == 0) return current
    val next = if (current == "0") d.toString() else current + d.toString()
    val dot = next.indexOf('.')
    val intLen = if (dot >= 0) dot else next.length
    val fracLen = if (dot >= 0) next.length - dot - 1 else 0
    return if (intLen <= MOVE_MAX_INT_DIGITS && fracLen <= 2) next else current
}
