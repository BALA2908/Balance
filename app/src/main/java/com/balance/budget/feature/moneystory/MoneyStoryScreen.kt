package com.balance.budget.feature.moneystory

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.balance.budget.core.haptics.rememberHaptics
import com.balance.budget.core.ui.components.parseColor
import com.balance.budget.core.ui.theme.CozyColors
import com.balance.budget.core.ui.theme.HeroAmountStyle
import com.balance.budget.core.util.Money
import com.balance.budget.domain.story.StoryCard
import kotlinx.coroutines.launch

/**
 * Full-screen, vertical-swipe "money story" (Wrapped-style). Tap to advance,
 * swipe up/down to move, count-ups per card, a gentle haptic on each card, and
 * per-card warm gradients. Closes on the X or after the last card's tap.
 */
@Composable
fun MoneyStoryScreen(
    onClose: () -> Unit,
    viewModel: MoneyStoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val cards = state.cards
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CozyColors.Espresso),
    ) {
        if (cards.isEmpty()) {
            Text(
                text = if (state.isLoading) "" else "Nothing to show yet.",
                color = CozyColors.Cream,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            val pagerState = rememberPagerState(pageCount = { cards.size })

            // Gentle haptic as each card arrives.
            LaunchedEffect(pagerState.currentPage) { haptics.tick() }

            VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                StoryPage(
                    card = cards[page],
                    isCurrent = pagerState.currentPage == page,
                    onTap = {
                        if (page < cards.lastIndex) {
                            scope.launch { pagerState.animateScrollToPage(page + 1) }
                        } else {
                            onClose()
                        }
                    },
                )
            }

            // Story progress bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                cards.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (i <= pagerState.currentPage) CozyColors.Cream
                                else CozyColors.Cream.copy(alpha = 0.25f)
                            ),
                    )
                }
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 8.dp),
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = CozyColors.Cream)
        }
    }
}

@Composable
private fun StoryPage(card: StoryCard, isCurrent: Boolean, onTap: () -> Unit) {
    val accent = parseColor(card.accentHex)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.45f), CozyColors.Espresso, CozyColors.Espresso),
                )
            )
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = card.emoji, style = HeroAmountStyle)
            Text(
                text = card.headline,
                style = MaterialTheme.typography.headlineMedium,
                color = CozyColors.Latte,
                textAlign = TextAlign.Center,
            )
            if (card.amountMinor != null) {
                val targetRupees = if (isCurrent) (card.amountMinor / 100L).toInt() else 0
                val animated by animateIntAsState(
                    targetValue = targetRupees,
                    animationSpec = tween(durationMillis = 900),
                    label = "story-amount",
                )
                Text(
                    text = Money.formatWhole(animated.toLong() * 100L),
                    style = HeroAmountStyle,
                    color = Color.White,
                )
            }
            Text(
                text = card.body,
                style = MaterialTheme.typography.titleLarge,
                color = CozyColors.Cream,
                textAlign = TextAlign.Center,
            )
        }
    }
}
