package com.balance.budget.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Cozy fintech palette — warm, dark-first, the anti-spreadsheet.
 * Base is a soft near-black espresso (not pure #000), the signature accent is a
 * warm amber. Category colors are muted/earthy so charts feel calm, not loud.
 */
object CozyColors {
    // Core surfaces (dark)
    val Espresso = Color(0xFF16130F)      // app background
    val Mocha = Color(0xFF211C17)         // raised surface / cards
    val Walnut = Color(0xFF2C261F)        // higher surface / sheet
    val Sand = Color(0xFF3A332A)          // outline / dividers

    // Signature accent — warm amber
    val Amber = Color(0xFFF0A868)
    val AmberSoft = Color(0xFFFFD8A8)
    val AmberDeep = Color(0xFFC97B3C)

    // Text
    val Cream = Color(0xFFF5EDE2)         // primary text on dark
    val Latte = Color(0xFFC9BCA9)         // secondary text
    val Taupe = Color(0xFF8C8175)         // tertiary / hints

    // Semantic
    val Sage = Color(0xFF8FB996)          // positive / under budget
    val Clay = Color(0xFFE08B7B)          // warning / over budget
    val Honey = Color(0xFFE8C06B)         // caution / approaching limit

    // Light surfaces (for the optional light theme)
    val Paper = Color(0xFFFBF6EF)
    val PaperRaised = Color(0xFFFFFFFF)
    val InkOnPaper = Color(0xFF2A241D)

    /** Earthy category palette, indexed by category color slot. */
    val categorySwatches = listOf(
        Color(0xFFE0795B), // Food   – terracotta
        Color(0xFF6FA8A0), // Travel – teal
        Color(0xFF8FB996), // Investment – sage
        Color(0xFFD9A441), // Petrol – ochre
        Color(0xFFB98BC9), // Shopping – mauve
        Color(0xFF7E97C9), // Bills – slate blue
        Color(0xFFE08B7B), // Health – clay
        Color(0xFFE8C06B), // Entertainment – honey
        Color(0xFF9C8C7A), // Other – taupe
    )
}
