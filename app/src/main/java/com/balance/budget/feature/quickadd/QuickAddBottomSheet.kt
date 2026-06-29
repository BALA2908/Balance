package com.balance.budget.feature.quickadd

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

/**
 * The Quick Add experience as a spring-driven modal bottom sheet. This single
 * host is reused by:
 *   - the dashboard FAB (in-app), and
 *   - [com.balance.budget.QuickAddActivity] (deep link / external shortcut).
 *
 * On a successful save the sheet hides cleanly, then [onDismiss] runs (hide the
 * sheet in-app, or finish the Activity for the deep-link path).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    onDismiss: () -> Unit,
    viewModel: QuickAddViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        // Swipe/scrim dismissal: reset so the next open starts clean. (For the
        // deep-link Activity the VM is destroyed on finish, so this is a no-op there.)
        onDismissRequest = {
            viewModel.consumeSaved()
            onDismiss()
        },
        sheetState = sheetState,
        // The crystal look: the sheet itself is invisible — only the two frosted
        // GlassPanels inside show, floating over a faint scrim. No bar/handle.
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.28f),
        dragHandle = null,
        tonalElevation = 0.dp,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        QuickAddSheetContent(
            viewModel = viewModel,
            onClose = {
                scope.launch {
                    sheetState.hide()
                    viewModel.consumeSaved() // reset only after it's off-screen
                    onDismiss()
                }
            },
        )
    }
}
