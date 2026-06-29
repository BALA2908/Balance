package com.balance.budget.domain.model

/** How an expense entered the app. */
enum class ExpenseSource { MANUAL, IMPORT, RECURRING }

/** Budget cadence. Monthly is the only one used today; weekly/custom reserved. */
enum class BudgetPeriod { MONTHLY }

/** How often a recurring item repeats. */
enum class RecurringCadence { WEEKLY, MONTHLY }
