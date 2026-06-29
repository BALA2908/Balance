package com.balance.budget.feature.quickadd

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.balance.budget.R
import com.balance.budget.core.haptics.rememberHaptics
import com.balance.budget.core.ui.components.PressScale
import com.balance.budget.core.ui.theme.Motion
import kotlinx.coroutines.delay

/**
 * The satisfying save micro-interaction:
 *   1. Tap → light haptic, the button presses in.
 *   2. While saving → a spinner.
 *   3. On success → CONFIRM haptic, the pill morphs toward a circle and a
 *      checkmark springs in with a touch of overshoot.
 *   4. After a beat → [onSavedAnimationEnd] fires so the sheet can dismiss.
 */
@Composable
fun SaveButton(
    enabled: Boolean,
    isSaving: Boolean,
    saved: Boolean,
    onClick: () -> Unit,
    onSavedAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()

    // 0f = full-width pill, 1f = collapsed circle (on success).
    val collapse by animateFloatAsState(
        targetValue = if (saved) 1f else 0f,
        animationSpec = Motion.morphSpring(),
        label = "save-collapse",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (saved) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "check-scale",
    )
    val corner by animateDpAsState(
        targetValue = if (saved) 40.dp else 20.dp,
        animationSpec = Motion.morphSpring(),
        label = "save-corner",
    )

    val container = if (enabled || saved) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val onContainer = if (enabled || saved) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    LaunchedEffect(saved) {
        if (saved) {
            haptics.confirm()
            delay(620)
            onSavedAnimationEnd()
        }
    }

    // Collapse width from full to ~80dp circle as `collapse` goes 0 -> 1.
    val widthFraction = 1f - (collapse * 0.78f)

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PressScale(
            onClick = {
                if (enabled && !isSaving && !saved) {
                    haptics.tick()
                    onClick()
                }
            },
            enabled = enabled && !isSaving && !saved,
            modifier = Modifier.fillMaxWidth(widthFraction),
        ) { pressed ->
            Surface(
                shape = RoundedCornerShape(corner),
                color = container,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .scale(if (pressed) 0.97f else 1f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        saved -> Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = onContainer,
                            modifier = Modifier
                                .size(32.dp)
                                .graphicsLayer {
                                    scaleX = checkScale
                                    scaleY = checkScale
                                },
                        )
                        isSaving -> CircularProgressIndicator(
                            color = onContainer,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp),
                        )
                        else -> Text(
                            text = stringResource(R.string.quick_add_save),
                            style = MaterialTheme.typography.titleLarge,
                            color = onContainer,
                        )
                    }
                }
            }
        }
    }
}
