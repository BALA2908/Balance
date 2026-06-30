package com.balance.budget.feature.quickadd

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import com.balance.budget.core.haptics.rememberHaptics
import com.balance.budget.core.ui.components.PressScale
import com.balance.budget.core.ui.components.parseColor
import com.balance.budget.domain.model.Tag

/**
 * Multi-select wrapping row of tag chips. Selected chips tint to the tag colour
 * with a small filled dot; tapping toggles. Optional — only shown when tags exist.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagChipRow(
    tags: List<Tag>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            TagChip(
                tag = tag,
                selected = tag.id in selectedIds,
                onClick = {
                    haptics.select()
                    onToggle(tag.id)
                },
            )
        }
    }
}

@Composable
private fun TagChip(tag: Tag, selected: Boolean, onClick: () -> Unit) {
    val accent = parseColor(tag.colorHex)
    val containerColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
        label = "tag-bg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tag-fg",
    )
    PressScale(onClick = onClick) { pressed ->
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            modifier = Modifier.padding(top = if (pressed) 1.dp else 0.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accent))
                Text(
                    text = "#${tag.name}",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
                )
            }
        }
    }
}
