package com.balance.budget.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.balance.budget.R
import com.balance.budget.core.ui.theme.CozyColors
import com.balance.budget.core.ui.theme.Motion
import kotlinx.coroutines.delay

/**
 * The branded opening moment — continues from the system splash coin. Over the
 * espresso background and a calm mesh wash, the coin scales + fades in, the
 * "Balance" wordmark rises beneath it, everything settles, then it fades out and
 * hands off to the app content. Tap anywhere to skip. Never shown under
 * reduce-motion (the caller gates that) — it's pure motion.
 */
@Composable
fun BalanceIntro(
    onFinished: () -> Unit,
) {
    val finish = rememberUpdatedState(onFinished)

    // Drive the whole sequence off one 0→1 progress so the coin, wordmark and the
    // closing fade stay in lockstep regardless of frame timing.
    val coin = remember { Animatable(0f) }
    val word = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coin.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.68f,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }
    LaunchedEffect(Unit) {
        delay(200)
        word.animateTo(1f, tween(durationMillis = 460, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(Motion.INTRO_MS.toLong() - 260)
        exit.animateTo(1f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
        finish.value()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CozyColors.Espresso)
            .alpha(1f - exit.value)
            .pointerInput(Unit) {
                // Tap anywhere skips straight to the app.
                awaitPointerEventScope {
                    awaitPointerEvent()
                    finish.value()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        MeshBackground(reduceMotion = false)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(168.dp)
                    .graphicsLayer {
                        val s = 0.72f + 0.28f * coin.value
                        scaleX = s
                        scaleY = s
                        alpha = coin.value
                    },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Balance",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 40.sp,
                color = CozyColors.Cream,
                modifier = Modifier.graphicsLayer {
                    alpha = word.value
                    translationY = (1f - word.value) * 22f
                },
            )
        }
    }
}
