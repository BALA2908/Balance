package com.balance.budget.feature.quickadd

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.balance.budget.core.haptics.rememberHaptics
import com.balance.budget.core.ui.components.PressScale
import com.balance.budget.core.ui.components.accountIconForKey
import com.balance.budget.core.ui.components.parseColor
import com.balance.budget.core.ui.theme.Motion
import com.balance.budget.domain.model.Account

/**
 * A compact horizontal row of wallet chips ("paid from"). Mirrors the category
 * chips but smaller and scrollable, sitting just under the category row in Quick
 * Add. Selecting tints to the account colour with a spring + haptic.
 */
@Composable
fun AccountChipRow(
    accounts: List<Account>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(accounts, key = { it.id }) { account ->
            AccountChip(
                account = account,
                selected = account.id == selectedId,
                onClick = {
                    haptics.select()
                    onSelect(account.id)
                },
            )
        }
    }
}

@Composable
private fun AccountChip(
    account: Account,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = parseColor(account.colorHex)
    val containerColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "acct-bg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "acct-fg",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = Motion.tapSpring(),
        label = "acct-scale",
    )

    PressScale(onClick = onClick) { pressed ->
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = containerColor,
            modifier = Modifier.scale(if (pressed) scale * 0.96f else scale),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(
                    imageVector = accountIconForKey(account.iconKey),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
                )
            }
        }
    }
}
