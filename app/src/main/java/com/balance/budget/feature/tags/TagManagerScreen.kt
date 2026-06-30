package com.balance.budget.feature.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.balance.budget.core.ui.components.CategorySwatchHexes
import com.balance.budget.core.ui.components.iconForKey
import com.balance.budget.core.ui.components.parseColor
import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.core.util.Money
import com.balance.budget.domain.analytics.TripRecap
import com.balance.budget.domain.model.Tag

@Composable
fun TagManagerScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: TagManagerViewModel = hiltViewModel(),
) {
    val tags by viewModel.tags.collectAsState()
    var editorFor by remember { mutableStateOf<EditorTarget?>(null) }
    var recapFor by remember { mutableStateOf<Tag?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Text(
                text = "Tags",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            TextButton(onClick = { editorFor = EditorTarget(null) }) { Text("Add") }
        }
        Text(
            text = "Cross-category labels like “Goa trip” or “Diwali”. Tap a tag for its recap.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        if (tags.isEmpty()) {
            Text(
                text = "No tags yet. Add one, then attach it to expenses in Quick Add.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, start = 4.dp),
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tags, key = { it.id }) { tag ->
                TagRow(
                    tag = tag,
                    onRecap = { recapFor = tag },
                    onEdit = { editorFor = EditorTarget(tag) },
                    onDelete = { viewModel.delete(tag) },
                )
            }
        }
    }

    editorFor?.let { target ->
        TagEditorSheet(
            initial = target.tag,
            onDismiss = { editorFor = null },
            onSave = { name, color ->
                val existing = target.tag
                if (existing == null) viewModel.create(name, color) else viewModel.rename(existing, name, color)
                editorFor = null
            },
        )
    }

    recapFor?.let { tag ->
        val recap by viewModel.recapFor(tag.id).collectAsState(initial = TripRecap.EMPTY)
        TripRecapSheet(tag = tag, recap = recap, onDismiss = { recapFor = null })
    }
}

private data class EditorTarget(val tag: Tag?)

@Composable
private fun TagRow(tag: Tag, onRecap: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onRecap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(parseColor(tag.colorHex)))
            Text(
                text = "#${tag.name}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            TextButton(onClick = onEdit) { Text("Edit") }
            TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TagEditorSheet(
    initial: Tag?,
    onSave: (name: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var color by remember { mutableStateOf(initial?.colorHex ?: CategorySwatchHexes.first()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp),
        ) {
            Text(
                text = if (initial == null) "New tag" else "Edit tag",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Colour",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
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
                onClick = { if (name.isNotBlank()) onSave(name, color) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) { Text(if (initial == null) "Create" else "Save", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripRecapSheet(tag: Tag, recap: TripRecap, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(parseColor(tag.colorHex)))
                Text(
                    text = "  #${tag.name} recap",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (recap.count == 0) {
                Text(
                    text = "Nothing tagged #${tag.name} yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                Text(
                    text = Money.format(recap.totalMinor),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
                val dates = listOfNotNull(recap.firstMillis, recap.lastMillis)
                val span = if (dates.size == 2 && recap.firstMillis != recap.lastMillis) {
                    "${DateTimeUtil.friendlyDate(recap.firstMillis!!)} – ${DateTimeUtil.friendlyDate(recap.lastMillis!!)}"
                } else recap.firstMillis?.let { DateTimeUtil.friendlyDate(it) } ?: ""
                Text(
                    text = "${recap.count} ${if (recap.count == 1) "expense" else "expenses"} · $span",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                recap.byCategory.forEach { c ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val color = parseColor(c.colorHex)
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(iconForKey(c.iconKey), contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
                        }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(c.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                            Box(
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth((c.percentOfTotal / 100.0).toFloat().coerceIn(0f, 1f))
                                        .height(4.dp).clip(RoundedCornerShape(2.dp)).background(color),
                                )
                            }
                        }
                        Text(
                            text = Money.format(c.spentMinor),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        }
    }
}
