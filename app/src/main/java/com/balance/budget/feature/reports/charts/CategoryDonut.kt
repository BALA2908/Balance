package com.balance.budget.feature.reports.charts

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.balance.budget.core.ui.components.parseColor
import com.balance.budget.core.ui.theme.Motion
import com.balance.budget.domain.analytics.CategorySlice

/**
 * A category-share donut in the earthy palette, sweeping in clockwise. Each
 * segment is a category's share of total spend; [center] is a slot (e.g. the
 * month total). Tiny gaps separate segments.
 */
@Composable
fun CategoryDonut(
    slices: List<CategorySlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 22.dp,
    reduceMotion: Boolean = false,
    center: @Composable () -> Unit = {},
) {
    val total = slices.sumOf { it.spentMinor }.coerceAtLeast(1L).toFloat()
    val sweep by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reduceMotion) snap() else Motion.chartDraw(),
        label = "donut-draw",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            val gapDegrees = 3f
            var startAngle = -90f
            slices.forEach { slice ->
                val fullSweep = 360f * (slice.spentMinor / total)
                val drawnSweep = (fullSweep * sweep - gapDegrees).coerceAtLeast(0f)
                drawArc(
                    color = parseColor(slice.colorHex),
                    startAngle = startAngle + gapDegrees / 2f,
                    sweepAngle = drawnSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                startAngle += fullSweep
            }
        }
        center()
    }
}
