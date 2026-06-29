package com.balance.budget.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.balance.budget.core.ui.theme.Motion

/**
 * A soft circular budget ring that sweeps in (via [Motion.chartDraw]) and whose
 * color animates between the budget states. The arc fills clockwise from the top;
 * it's clamped to a full circle even when over budget (the color carries the
 * "over" signal). Center [content] is a slot — typically a % or remaining amount.
 */
@Composable
fun BudgetRing(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = color.copy(alpha = 0.16f),
    strokeWidth: Dp = 12.dp,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit = {},
) {
    val target = fraction.coerceIn(0f, 1f)
    val sweep by animateFloatAsState(
        targetValue = target,
        animationSpec = if (reduceMotion) snap() else Motion.chartDraw(),
        label = "budget-ring-sweep",
    )
    val animatedColor by animateColorAsState(targetValue = color, label = "budget-ring-color")

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            // Track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Progress (from 12 o'clock, clockwise)
            drawArc(
                color = animatedColor,
                startAngle = -90f,
                sweepAngle = 360f * sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}
