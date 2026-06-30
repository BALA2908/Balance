package com.balance.budget.feature.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.balance.budget.core.ui.components.iconForKey
import com.balance.budget.core.ui.components.parseColor
import com.balance.budget.domain.model.Category
import com.balance.budget.domain.model.CategoryRule
import com.balance.budget.feature.quickadd.CategoryChipRow

@Composable
fun RulesScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: RulesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var editorFor by remember { mutableStateOf<EditorTarget?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Text(
                text = "Rules",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            IconButton(onClick = { if (state.categories.isNotEmpty()) editorFor = EditorTarget(null) }) {
                Icon(Icons.Rounded.Add, contentDescription = "New rule")
            }
        }
        Text(
            text = "When a merchant or note contains your text, auto-pick a category. Rules win over learned guesses.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (state.rules.isEmpty()) {
                item {
                    Text(
                        text = "No rules yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                    )
                }
            }
            items(state.rules, key = { it.id }) { rule ->
                RuleRow(
                    rule = rule,
                    category = state.categoriesById[rule.categoryId],
                    onEdit = { editorFor = EditorTarget(rule) },
                    onDelete = { viewModel.delete(rule) },
                )
            }

            if (state.learned.isNotEmpty()) {
                item {
                    Text(
                        text = "Learned patterns",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                    )
                }
                items(state.learned, key = { "learned-${it.label}" }) { sug ->
                    val cat = state.categoriesById[sug.categoryId] ?: return@items
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CategoryDot(cat)
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("“${sug.label}”", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                                Text("usually ${cat.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { viewModel.promote(sug) }) { Text("Make a rule") }
                        }
                    }
                }
            }
        }
    }

    editorFor?.let { target ->
        RuleEditorSheet(
            initial = target.rule,
            categories = state.categories,
            onDismiss = { editorFor = null },
            onSave = { pattern, categoryId ->
                val existing = target.rule
                if (existing == null) viewModel.add(pattern, categoryId) else viewModel.update(existing, pattern, categoryId)
                editorFor = null
            },
        )
    }
}

private data class EditorTarget(val rule: CategoryRule?)

@Composable
private fun RuleRow(rule: CategoryRule, category: Category?, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (category != null) CategoryDot(category)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text("contains “${rule.pattern}”", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("→ ${category?.name ?: "—"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onEdit) { Text("Edit") }
            TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun CategoryDot(category: Category) {
    val color = parseColor(category.colorHex)
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(iconForKey(category.iconKey), contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleEditorSheet(
    initial: CategoryRule?,
    categories: List<Category>,
    onSave: (pattern: String, categoryId: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pattern by remember { mutableStateOf(initial?.pattern ?: "") }
    var categoryId by remember { mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (initial == null) "New rule" else "Edit rule",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            OutlinedTextField(
                value = pattern,
                onValueChange = { pattern = it },
                label = { Text("When text contains…") },
                placeholder = { Text("e.g. swiggy") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Category", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            CategoryChipRow(
                categories = categories,
                selectedId = categoryId,
                onSelect = { categoryId = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { val c = categoryId; if (pattern.isNotBlank() && c != null) onSave(pattern, c) },
                enabled = pattern.isNotBlank() && categoryId != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (initial == null) "Create" else "Save", style = MaterialTheme.typography.titleMedium) }
        }
    }
}
