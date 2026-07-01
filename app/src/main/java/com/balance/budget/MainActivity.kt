package com.balance.budget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.balance.budget.core.ui.components.BalanceIntro
import com.balance.budget.core.ui.theme.Motion
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.budget.core.ui.theme.BalanceTheme
import com.balance.budget.data.preferences.SettingsRepository
import com.balance.budget.domain.model.ThemeMode
import com.balance.budget.feature.onboarding.OnboardingScreen
import com.balance.budget.navigation.AppScaffold
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The single in-app host. Resolves the theme + first-launch state, then shows
 * either onboarding or the main app shell. The external Quick Add path goes
 * through [QuickAddActivity] instead.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { BalanceRoot() }
    }
}

data class RootUiState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val firstLaunchComplete: Boolean = false,
    val reduceMotion: Boolean = false,
    val loaded: Boolean = false,
)

@HiltViewModel
class RootViewModel @Inject constructor(
    settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<RootUiState> = combine(
        settings.themeMode,
        settings.firstLaunchComplete,
        settings.reduceMotion,
    ) { theme, firstDone, reduceMotion -> RootUiState(theme, firstDone, reduceMotion, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootUiState())
}

@Composable
fun BalanceRoot(viewModel: RootViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val darkTheme = when (state.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    // Branded opening intro — shown once per cold start, over the loaded content.
    // Skipped entirely under reduce-motion. Remembered at the activity root so it
    // never replays on recomposition or navigation.
    var introDone by remember { mutableStateOf(false) }

    BalanceTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background,
        ) {
            if (state.loaded) {
                // Gentle app-open entrance — a calm fade + rise, the first time content
                // appears. Honors reduce-motion (shows instantly).
                val appeared = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = appeared,
                    enter = if (state.reduceMotion) {
                        EnterTransition.None
                    } else {
                        fadeIn(tween(Motion.SCREEN_ENTER_MS)) +
                            slideInVertically(tween(Motion.SCREEN_ENTER_MS)) { it / 14 }
                    },
                ) {
                    if (!state.firstLaunchComplete) OnboardingScreen() else AppScaffold(reduceMotion = state.reduceMotion)
                }
            }

            if (state.loaded && !state.reduceMotion && !introDone) {
                BalanceIntro(onFinished = { introDone = true })
            }
        }
    }
}
