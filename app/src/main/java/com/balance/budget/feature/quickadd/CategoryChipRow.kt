package com.balance.budget.feature.quickadd

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.balance.budget.R
import com.balance.budget.core.haptics.rememberHaptics
import com.balance.budget.core.ui.components.PressScale
import com.balance.budget.core.ui.components.iconForKey
import com.balance.budget.core.ui.components.parseColor
import com.balance.budget.core.ui.theme.Motion
import com.balance.budget.domain.model.Category

/**
 * Wrapping row of category chips. The selected chip lifts (spring scale),
 * tints to its category color, and fires a selection haptic. Tapping is the
 * one-touch way to categorize an expense.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChipRow(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            CategoryChip(
                category = category,
                selected = category.id == selectedId,
                onClick = {
                    haptics.select()
                    onSelect(category.id)
                },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = parseColor(category.colorHex)
    val containerColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "chip-bg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chip-fg",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = Motion.tapSpring(),
        label = "chip-scale",
    )
    val cd = stringResource(R.string.cd_category, category.name)

    PressScale(onClick = onClick) { pressed ->
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
            modifier = Modifier
                .scale(if (pressed) scale * 0.96f else scale)
                .semantics {
                    this.selected = selected
                    contentDescription = cd
                },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Icon(
                    imageVector = iconForKey(category.iconKey),
                    contentDescription = null,
                    tint = if (selected) accent else contentColor,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                )
            }
        }
    }
}
