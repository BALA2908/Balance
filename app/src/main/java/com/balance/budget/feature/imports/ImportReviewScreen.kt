package com.balance.budget.feature.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.core.util.Money
import com.balance.budget.domain.model.Category
import com.balance.budget.domain.model.ImportCandidate
import com.balance.budget.feature.quickadd.CategoryChipRow

@Composable
fun ImportReviewScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: ImportReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

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
                    text = "Review imports",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        if (state.isEmpty) {
            item { AllCaughtUp() }
        } else {
            items(state.candidates, key = { it.id }) { candidate ->
                CandidateCard(
                    candidate = candidate,
                    categories = state.categories,
                    onConfirm = { categoryId -> viewModel.confirm(candidate, categoryId) },
                    onDismiss = { viewModel.dismiss(candidate) },
                )
            }
        }
    }
}

@Composable
private fun CandidateCard(
    candidate: ImportCandidate,
    categories: List<Category>,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedCategoryId by remember(candidate.id) { mutableStateOf(candidate.suggestedCategoryId) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = Money.formatWhole(candidate.amountMinor),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${candidate.merchant ?: "UPI payment"} · ${candidate.sourceApp} · ${DateTimeUtil.timeOfDay(candidate.postedAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CategoryChipRow(
                categories = categories,
                selectedId = selectedCategoryId,
                onSelect = { selectedCategoryId = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { selectedCategoryId?.let(onConfirm) },
                    enabled = selectedCategoryId != null,
                    modifier = Modifier.weight(1f),
                ) { Text("Add expense") }
            }
        }
    }
}

@Composable
private fun AllCaughtUp() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("🎉", style = MaterialTheme.typography.displayMedium)
        Text(
            "All caught up",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "Confirmed UPI payments land in your history. New ones to review will appear here.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
