package com.balance.budget.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * A frosted-glass panel: a translucent surface with a soft rounded shape and a
 * subtle light top-edge highlight. Sits over the blurred/transparent backdrop of
 * the Quick Add popup so the backdrop tints through — the "crystal" look. Adapts
 * to the active (dark/light) theme.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            // Translucent enough to read as "glass" over the blurred backdrop, but
            // opaque enough that the card's own content stays crisp even on devices
            // where cross-window blur is weak or disabled.
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                        Color.Transparent,
                    ),
                ),
                shape = shape,
            ),
        content = content,
    )
}
