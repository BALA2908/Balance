package com.balance.budget.domain.model

/** How an expense entered the app. */
enum class ExpenseSource { MANUAL, IMPORT, RECURRING }

/** A wallet/payment-method kind. Cash-first: CASH is the seeded default. */
enum class AccountType { CASH, BANK, CARD, WALLET, OTHER }

/** Budget cadence. Monthly is the only one used today; weekly/custom reserved. */
enum class BudgetPeriod { MONTHLY }

/** How often a recurring item repeats. */
enum class RecurringCadence { WEEKLY, MONTHLY }
