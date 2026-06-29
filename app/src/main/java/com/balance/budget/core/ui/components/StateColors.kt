package com.balance.budget.core.ui.components

import androidx.compose.ui.graphics.Color
import com.balance.budget.core.ui.theme.CozyColors
import com.balance.budget.domain.model.BudgetState

/** The calm semantic color for a budget state — Sage / Honey / Clay. */
fun BudgetState.tint(): Color = when (this) {
    BudgetState.UNDER -> CozyColors.Sage
    BudgetState.APPROACHING -> CozyColors.Honey
    BudgetState.OVER -> CozyColors.Clay
}
