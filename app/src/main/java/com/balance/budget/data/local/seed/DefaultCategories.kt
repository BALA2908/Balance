package com.balance.budget.data.local.seed

import com.balance.budget.data.local.entity.CategoryEntity

/**
 * The starter categories, seeded on first launch. The user can edit, recolor,
 * reorder, or archive any of these in Settings. colorHex values map to the
 * earthy swatches in CozyColors.categorySwatches.
 */
object DefaultCategories {

    val list: List<CategoryEntity> = listOf(
        cat("Food", "food", "#E0795B", 0),
        cat("Travel", "travel", "#6FA8A0", 1),
        cat("Investment", "investment", "#8FB996", 2),
        cat("Petrol", "petrol", "#D9A441", 3),
        cat("Shopping", "shopping", "#B98BC9", 4),
        cat("Bills", "bills", "#7E97C9", 5),
        cat("Health", "health", "#E08B7B", 6),
        cat("Entertainment", "entertainment", "#E8C06B", 7),
        cat("Sports", "sports", "#7FB069", 8),
        cat("Other", "other", "#9C8C7A", 9),
    )

    private fun cat(name: String, icon: String, color: String, order: Int) =
        CategoryEntity(
            name = name,
            iconKey = icon,
            colorHex = color,
            isDefault = true,
            isArchived = false,
            sortOrder = order,
        )
}
