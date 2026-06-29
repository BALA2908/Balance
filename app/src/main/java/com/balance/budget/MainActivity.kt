package com.balance.budget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    val loaded: Boolean = false,
)

@HiltViewModel
class RootViewModel @Inject constructor(
    settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<RootUiState> = combine(
        settings.themeMode,
        settings.firstLaunchComplete,
    ) { theme, firstDone -> RootUiState(theme, firstDone, loaded = true) }
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
    BalanceTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background,
        ) {
            when {
                !state.loaded -> Unit // brief: wait for the persisted prefs before deciding
                !state.firstLaunchComplete -> OnboardingScreen()
                else -> AppScaffold()
            }
        }
    }
}
