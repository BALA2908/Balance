package com.balance.budget.feature.reports.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.balance.budget.core.ui.theme.Motion
import kotlin.math.max

/**
 * A cozy cumulative-spend area chart that draws in left-to-right. The optional
 * dashed budget line shows where the month's limit sits. Needs ≥2 points; the
 * caller shows a placeholder otherwise.
 */
@Composable
fun SpendTrendChart(
    cumulative: List<Long>,
    budgetMinor: Long?,
    lineColor: Color,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
) {
    if (cumulative.size < 2) return
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reduceMotion) snap() else Motion.chartDraw(),
        label = "trend-draw",
    )
    val maxValue = max(cumulative.last(), budgetMinor ?: 0L).coerceAtLeast(1L).toFloat()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val n = cumulative.size
        fun pointAt(i: Int): Offset {
            val x = w * i / (n - 1)
            val y = h - h * (cumulative[i].toFloat() / maxValue)
            return Offset(x, y)
        }

        // Budget reference line (dashed).
        budgetMinor?.let { b ->
            val by = h - h * (b.toFloat() / maxValue)
            drawLine(
                color = lineColor.copy(alpha = 0.35f),
                start = Offset(0f, by),
                end = Offset(w, by),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
            )
        }

        val linePath = Path().apply {
            moveTo(pointAt(0).x, pointAt(0).y)
            for (i in 1 until n) lineTo(pointAt(i).x, pointAt(i).y)
        }
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        clipRect(right = w * progress) {
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent),
                ),
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
