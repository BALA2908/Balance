package com.balance.budget.navigation

/** All navigation destinations. The first four are the bottom-nav tabs. */
object Routes {
    // Bottom-nav tabs
    const val DASHBOARD = "dashboard"
    const val REPORTS = "reports"     // Phase 3 (ComingSoon placeholder until then)
    const val HISTORY = "history"     // Phase 4 (ComingSoon placeholder until then)
    const val SETTINGS = "settings"

    // Pushed (non-tab) routes
    const val BUDGETS = "budgets"
    const val CATEGORIES = "categories"
    const val ACCOUNTS = "accounts"
    const val TAGS = "tags"
    const val RULES = "rules"
    const val RECURRING = "recurring"
    const val BILLS = "bills"
    const val GOALS = "goals"
    const val NET_WORTH = "net_worth"
    const val ONBOARDING = "onboarding"
    const val IMPORT_REVIEW = "import_review"
    const val ASK = "ask"
    const val MONEY_STORY = "money_story"
}
