package com.balance.budget.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.balance.budget.core.ui.components.parseColor
import com.balance.budget.core.ui.theme.Motion
import com.balance.budget.core.util.Money
import com.balance.budget.domain.analytics.CategorySlice

/**
 * A compact "where it went" strip: the top categories, each with its amount and
 * a thin share bar in the category's own earthy color. Bars grow in smoothly.
 */
@Composable
fun CategoryBreakdownStrip(
    slices: List<CategorySlice>,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        slices.forEach { slice ->
            CategoryShareRow(slice, reduceMotion)
        }
    }
}

@Composable
private fun CategoryShareRow(slice: CategorySlice, reduceMotion: Boolean) {
    val color = parseColor(slice.colorHex)
    val targetFraction = (slice.percentOfTotal / 100.0).toFloat().coerceIn(0f, 1f)
    val fraction by animateFloatAsState(
        targetValue = if (reduceMotion) targetFraction else targetFraction,
        animationSpec = Motion.chartDraw(),
        label = "cat-share-${slice.categoryId}",
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = slice.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            Text(
                text = Money.format(slice.spentMinor),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        // Thin share bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}
