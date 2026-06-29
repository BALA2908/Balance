package com.balance.budget.domain.model

/**
 * How a budget is tracking, for color + tone in the UI:
 *   UNDER       — comfortably within budget (Sage)
 *   APPROACHING — past 80% of the limit (Honey)
 *   OVER        — spent more than the limit (Clay)
 */
enum class BudgetState { UNDER, APPROACHING, OVER;

    companion object {
        /** Threshold (fraction of budget) at which we switch to APPROACHING. */
        const val APPROACHING_FRACTION = 0.8

        /**
         * Classify spend against a budget. A null/non-positive budget is treated
         * as UNDER (nothing to exceed).
         */
        fun of(spentMinor: Long, budgetMinor: Long?): BudgetState = when {
            budgetMinor == null || budgetMinor <= 0L -> UNDER
            spentMinor > budgetMinor -> OVER
            spentMinor.toDouble() / budgetMinor >= APPROACHING_FRACTION -> APPROACHING
            else -> UNDER
        }
    }
}
