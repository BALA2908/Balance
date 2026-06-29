package com.balance.budget.core.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import com.balance.budget.core.ui.theme.HeroAmountStyle
import com.balance.budget.core.ui.theme.Motion
import com.balance.budget.core.util.Money

/**
 * A money amount that counts up from its previous value to the new one — the
 * signature dashboard animation. Animation runs on the rupee value (whole
 * rupees) so it reads cleanly, then formats with the ₹ grouping.
 *
 * @param amountMinor target amount in paise
 * @param whole when true, render rounded to whole rupees (for hero numbers)
 */
@Composable
fun CountUpAmount(
    amountMinor: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = HeroAmountStyle,
    color: Color = LocalContentColor.current,
    whole: Boolean = true,
) {
    // Animate the rupee integer; paise precision isn't worth animating.
    val targetRupees = remember(amountMinor) { (amountMinor / 100L).toInt() }
    val animated by animateIntAsState(
        targetValue = targetRupees,
        animationSpec = Motion.countUp(),
        label = "count-up-amount",
    )
    val text = if (whole) {
        Money.formatWhole(animated.toLong() * 100L)
    } else {
        Money.format(animated.toLong() * 100L)
    }
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
