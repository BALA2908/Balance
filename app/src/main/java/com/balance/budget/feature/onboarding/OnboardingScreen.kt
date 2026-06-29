package com.balance.budget.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.balance.budget.core.ui.components.AmountEntrySheet
import com.balance.budget.core.ui.components.MeshBackground
import com.balance.budget.core.ui.theme.HeroAmountStyle

/**
 * Warm first-launch welcome. The whole app works without this, but setting a
 * budget here makes safe-to-spend meaningful from day one.
 */
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    var showEntry by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        MeshBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "🪙", style = HeroAmountStyle)
            Text(
                text = "Welcome to Balance",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "What feels like a comfortable amount to spend this month? " +
                    "I'll use it to show your guilt-free, safe-to-spend number each day.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
            )
            Button(
                onClick = { showEntry = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Set my monthly budget", style = MaterialTheme.typography.titleMedium)
            }
            TextButton(
                onClick = { viewModel.skip() },
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text("Skip for now", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showEntry) {
        AmountEntrySheet(
            title = "Monthly budget",
            saveLabel = "Start tracking",
            onSave = { viewModel.complete(it); showEntry = false },
            onDismiss = { showEntry = false },
        )
    }
}
