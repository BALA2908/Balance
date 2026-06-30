package com.balance.budget.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.balance.budget.core.ui.components.AccountIconCatalog
import com.balance.budget.core.ui.components.CategorySwatchHexes
import com.balance.budget.core.ui.components.accountIconForKey
import com.balance.budget.core.ui.components.parseColor
import com.balance.budget.core.ui.components.rememberDragDropState
import com.balance.budget.domain.model.Account
import com.balance.budget.domain.model.AccountType
import java.util.Locale

@Composable
fun AccountManagerScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: AccountManagerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var editorFor by remember { mutableStateOf<EditorTarget?>(null) }

    val ordered = remember { mutableStateListOf<Account>() }
    val listState = rememberLazyListState()
    val dragState = rememberDragDropState(listState) { from, to ->
        if (from in ordered.indices && to in ordered.indices) {
            ordered.add(to, ordered.removeAt(from))
        }
    }
    if (dragState.draggingItemIndex == null &&
        ordered.map { it.id }.toSet() != state.active.map { it.id }.toSet()
    ) {
        ordered.clear()
        ordered.addAll(state.active)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Accounts",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            TextButton(onClick = { editorFor = EditorTarget(null) }) { Text("Add") }
        }
        Text(
            text = "Wallets you pay from. Tap ★ to set the default Quick Add picks. Long-press a handle to reorder.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dragState) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragState.onDragStart(it) },
                        onDrag = { change, amount -> change.consume(); dragState.onDrag(amount) },
                        onDragEnd = {
                            dragState.onDragEnd()
                            viewModel.reorder(ordered.map { it.id })
                        },
                        onDragCancel = { dragState.onDragEnd() },
                    )
                },
        ) {
            itemsIndexed(ordered, key = { _, a -> a.id }) { index, account ->
                val dragging = dragState.draggingItemIndex == index
                AccountRow(
                    account = account,
                    onEdit = { editorFor = EditorTarget(account) },
                    onSetDefault = { viewModel.setDefault(account) },
                    onArchive = { viewModel.setArchived(account, true) },
                    archiveEnabled = state.canArchive && !account.isDefault,
                    modifier = if (dragging) {
                        Modifier.zIndex(1f).graphicsLayer { translationY = dragState.draggingItemOffset }
                    } else {
                        Modifier.animateItem()
                    },
                    elevated = dragging,
                )
            }

            if (state.archived.isNotEmpty()) {
                item(key = "archived-label") {
                    Text(
                        text = "Archived",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                    )
                }
                items(state.archived, key = { "arch-${it.id}" }) { account ->
                    ArchivedRow(account = account, onRestore = { viewModel.setArchived(account, false) })
                }
            }
            item(key = "footer") { Spacer(Modifier.size(24.dp)) }
        }
    }

    editorFor?.let { target ->
        AccountEditorSheet(
            initial = target.account,
            onDismiss = { editorFor = null },
            onSave = { name, type, icon, color, balance ->
                val existing = target.account
                if (existing == null) viewModel.create(name, type, icon, color, balance)
                else viewModel.save(existing, name, type, icon, color, balance)
                editorFor = null
            },
        )
    }
}

private data class EditorTarget(val account: Account?)

@Composable
private fun AccountRow(
    account: Account,
    onEdit: () -> Unit,
    onSetDefault: () -> Unit,
    onArchive: () -> Unit,
    archiveEnabled: Boolean,
    elevated: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (elevated) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        tonalElevation = if (elevated) 6.dp else 0.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountAvatar(account)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    text = account.type.label() + if (account.isDefault) " · default" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onSetDefault) {
                Icon(
                    imageVector = if (account.isDefault) Icons.Rounded.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (account.isDefault) "Default account" else "Make default",
                    tint = if (account.isDefault) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onEdit) { Text("Edit") }
            if (archiveEnabled) {
                IconButton(onClick = onArchive) {
                    Icon(Icons.Outlined.Archive, contentDescription = "Archive ${account.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                Icons.Outlined.DragHandle,
                contentDescription = "Reorder ${account.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun ArchivedRow(account: Account, onRestore: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountAvatar(account, dimmed = true)
        Text(
            text = account.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        TextButton(onClick = onRestore) { Text("Restore") }
    }
}

@Composable
private fun AccountAvatar(account: Account, dimmed: Boolean = false) {
    val color = parseColor(account.colorHex)
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape)
            .background(color.copy(alpha = if (dimmed) 0.10f else 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = accountIconForKey(account.iconKey),
            contentDescription = null,
            tint = if (dimmed) color.copy(alpha = 0.6f) else color,
            modifier = Modifier.size(20.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AccountEditorSheet(
    initial: Account?,
    onSave: (name: String, type: AccountType, iconKey: String, colorHex: String, balanceMinor: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: AccountType.CASH) }
    var icon by remember { mutableStateOf(initial?.iconKey ?: AccountIconCatalog.first()) }
    var color by remember { mutableStateOf(initial?.colorHex ?: CategorySwatchHexes.first()) }
    var balanceText by remember {
        mutableStateOf(initial?.openingBalanceMinor?.let { com.balance.budget.core.util.Money.formatPlain(it).replace(",", "") } ?: "")
    }
    val balanceMinor = if (balanceText.isBlank()) null else com.balance.budget.core.util.Money.parseToMinor(balanceText)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 16.dp),
        ) {
            Text(
                text = if (initial == null) "New account" else "Edit account",
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
            OutlinedTextField(
                value = balanceText,
                onValueChange = { new -> balanceText = new.filter { it.isDigit() || it == '.' } },
                label = { Text("Current balance (₹, optional)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            EditorLabel("Type")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountType.entries.forEach { t ->
                    FilterChip(
                        selected = t == type,
                        onClick = { type = t },
                        label = { Text(t.label()) },
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }

            EditorLabel("Icon")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccountIconCatalog.forEach { key ->
                    val selected = key == icon
                    val tint = parseColor(color)
                    Box(
                        modifier = Modifier.padding(bottom = 10.dp).size(48.dp).clip(CircleShape)
                            .background(if (selected) tint.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(width = if (selected) 2.dp else 0.dp, color = if (selected) tint else MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                            .clickable { icon = key },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = accountIconForKey(key),
                            contentDescription = key,
                            tint = if (selected) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
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
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Button(
                onClick = { if (name.isNotBlank()) onSave(name, type, icon, color, balanceMinor) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text(if (initial == null) "Create" else "Save", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun EditorLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

private fun AccountType.label(): String =
    name.lowercase(Locale.US).replaceFirstChar { it.uppercase(Locale.US) }
