package com.balance.budget.feature.goals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.balance.budget.core.ui.components.AmountEntrySheet
import com.balance.budget.core.ui.components.CategoryIconCatalog
import com.balance.budget.core.ui.components.CategorySwatchHexes
import com.balance.budget.core.ui.components.iconForKey
import com.balance.budget.core.ui.components.parseColor
import com.balance.budget.core.util.Money
import com.balance.budget.domain.model.SavingsGoal

@Composable
fun SavingsGoalsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: SavingsGoalsViewModel = hiltViewModel(),
) {
    val goals by viewModel.goals.collectAsState()
    var editorFor by remember { mutableStateOf<EditorTarget?>(null) }
    var contributeTo by remember { mutableStateOf<SavingsGoal?>(null) }
    var manageGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Text(
                text = "Savings goals",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            IconButton(onClick = { editorFor = EditorTarget(null) }) { Icon(Icons.Rounded.Add, contentDescription = "New goal") }
        }
        Text(
            text = "Set aside for what matters. Tap a goal to add money.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        if (goals.isEmpty()) {
            Text(
                text = "No goals yet. Add one — an emergency fund, a trip, a new phone.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, start = 4.dp),
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(goals, key = { it.id }) { goal ->
                GoalCard(goal = goal, onClick = { contributeTo = goal }, onLongClick = { manageGoal = goal })
            }
        }
    }

    editorFor?.let { target ->
        GoalEditorSheet(
            initial = target.goal,
            onDismiss = { editorFor = null },
            onSave = { name, icon, color, target2 ->
                val existing = target.goal
                if (existing == null) viewModel.add(name, icon, color, target2)
                else viewModel.save(existing, name, icon, color, target2)
                editorFor = null
            },
        )
    }

    contributeTo?.let { goal ->
        AmountEntrySheet(
            title = "Add to ${goal.name}",
            saveLabel = "Add",
            onSave = { viewModel.contribute(goal.id, it); contributeTo = null },
            onDismiss = { contributeTo = null },
        )
    }

    manageGoal?.let { goal ->
        ManageGoalSheet(
            goal = goal,
            onEdit = { manageGoal = null; editorFor = EditorTarget(goal) },
            onDelete = { viewModel.delete(goal.id); manageGoal = null },
            onDismiss = { manageGoal = null },
        )
    }
}

private data class EditorTarget(val goal: SavingsGoal?)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GoalCard(goal: SavingsGoal, onClick: () -> Unit, onLongClick: () -> Unit) {
    val color = parseColor(goal.colorHex)
    val progress by animateFloatAsState(targetValue = goal.fraction, label = "goal-progress")
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(iconForKey(goal.iconKey), contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(goal.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        text = "${Money.format(goal.savedMinor)} of ${Money.format(goal.targetMinor)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (goal.isComplete) "Done 🎉" else "${(goal.fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (goal.isComplete) MaterialTheme.colorScheme.primary else color,
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 0.dp)
                    .clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(progress).height(8.dp)
                        .clip(RoundedCornerShape(4.dp)).background(color),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GoalEditorSheet(
    initial: SavingsGoal?,
    onSave: (name: String, iconKey: String, colorHex: String, targetMinor: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var icon by remember { mutableStateOf(initial?.iconKey ?: "savings") }
    var color by remember { mutableStateOf(initial?.colorHex ?: CategorySwatchHexes.first()) }
    var targetText by remember {
        mutableStateOf(initial?.let { Money.formatPlain(it.targetMinor).replace(",", "") } ?: "")
    }
    val targetMinor = Money.parseToMinor(targetText) ?: 0L

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (initial == null) "New goal" else "Edit goal",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = targetText,
                onValueChange = { new -> targetText = new.filter { it.isDigit() || it == '.' } },
                label = { Text("Target (₹)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            EditorLabel("Icon")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CategoryIconCatalog.forEach { key ->
                    val selected = key == icon
                    val tint = parseColor(color)
                    Box(
                        modifier = Modifier.padding(bottom = 10.dp).size(46.dp).clip(CircleShape)
                            .background(if (selected) tint.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(width = if (selected) 2.dp else 0.dp, color = if (selected) tint else MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                            .clickable { icon = key },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(iconForKey(key), contentDescription = key, tint = if (selected) tint else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(21.dp))
                    }
                }
            }
            EditorLabel("Colour")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategorySwatchHexes.forEach { hex ->
                    val swatch = parseColor(hex)
                    val selected = hex == color
                    Box(
                        modifier = Modifier.padding(bottom = 12.dp).size(40.dp).clip(CircleShape).background(swatch)
                            .border(width = if (selected) 3.dp else 0.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                            .clickable { color = hex },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Button(
                onClick = { if (name.isNotBlank() && targetMinor > 0) onSave(name, icon, color, targetMinor) },
                enabled = name.isNotBlank() && targetMinor > 0,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text(if (initial == null) "Create" else "Save", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageGoalSheet(
    goal: SavingsGoal,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
        ) {
            Text(goal.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 8.dp))
            TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit goal") }
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun EditorLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}
