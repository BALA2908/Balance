package com.balance.budget.feature.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.balance.budget.R
import com.balance.budget.core.haptics.rememberHaptics
import com.balance.budget.core.ui.components.PressScale

/**
 * A big, finger-friendly number pad. Pre-focused conceptually (it's the first
 * thing you touch). Every key gives a light haptic tick on the S24, and presses
 * spring-scale for tactility. Layout: 1-9, then ".", 0, backspace.
 */
@Composable
fun NumberPad(
    onDigit: (Int) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val haptics = rememberHaptics()
    val rows = listOf(
        listOf(Key.Digit(1), Key.Digit(2), Key.Digit(3)),
        listOf(Key.Digit(4), Key.Digit(5), Key.Digit(6)),
        listOf(Key.Digit(7), Key.Digit(8), Key.Digit(9)),
        listOf(Key.Decimal, Key.Digit(0), Key.Backspace),
    )
    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    PadKey(
                        key = key,
                        compact = compact,
                        modifier = Modifier.weight(1f),
                        onPress = {
                            haptics.tick()
                            when (key) {
                                is Key.Digit -> onDigit(key.value)
                                Key.Decimal -> onDecimal()
                                Key.Backspace -> onBackspace()
                            }
                        },
                    )
                }
            }
        }
    }
}

private sealed interface Key {
    data class Digit(val value: Int) : Key
    data object Decimal : Key
    data object Backspace : Key
}

@Composable
private fun PadKey(
    key: Key,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val backspaceCd = stringResource(R.string.cd_delete_digit)
    PressScale(onClick = onPress, modifier = modifier) { pressed ->
        Surface(
            shape = RoundedCornerShape(if (compact) 18.dp else 20.dp),
            // Frosted/translucent keys for the glass keypad; solid otherwise.
            color = if (compact) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (compact) 2.4f else 1.6f)
                .scale(if (pressed) 0.94f else 1f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                when (key) {
                    is Key.Digit -> Text(
                        text = key.value.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Key.Decimal -> Text(
                        text = ".",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Key.Backspace -> Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = backspaceCd,
                        modifier = Modifier.semantics { },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
