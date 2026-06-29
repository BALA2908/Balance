package com.balance.budget.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.toColorInt

/** Maps a category's stable iconKey to a Material icon. Unknown keys → a dot. */
fun iconForKey(iconKey: String): ImageVector = when (iconKey) {
    "food" -> Icons.Outlined.Restaurant
    "travel" -> Icons.Outlined.DirectionsCar
    "investment" -> Icons.Outlined.TrendingUp
    "petrol" -> Icons.Outlined.LocalGasStation
    "shopping" -> Icons.Outlined.ShoppingBag
    "bills" -> Icons.Outlined.Receipt
    "health" -> Icons.Outlined.Favorite
    "entertainment" -> Icons.Outlined.MovieFilter
    "sports" -> Icons.Outlined.SportsBasketball
    // Extra choices offered in the icon picker:
    "coffee" -> Icons.Outlined.Coffee
    "groceries" -> Icons.Outlined.LocalGroceryStore
    "home" -> Icons.Outlined.Cottage
    "pets" -> Icons.Outlined.Pets
    "education" -> Icons.Outlined.School
    "gift" -> Icons.Outlined.CardGiftcard
    "fitness" -> Icons.Outlined.FitnessCenter
    "savings" -> Icons.Outlined.Savings
    "phone" -> Icons.Outlined.Smartphone
    "flight" -> Icons.Outlined.Flight
    "subscriptions" -> Icons.Outlined.Subscriptions
    "work" -> Icons.Outlined.Work
    "transit" -> Icons.Outlined.DirectionsBus
    "music" -> Icons.Outlined.MusicNote
    "games" -> Icons.Outlined.SportsEsports
    "beauty" -> Icons.Outlined.Spa
    "kids" -> Icons.Outlined.ChildCare
    "charity" -> Icons.Outlined.VolunteerActivism
    else -> Icons.Filled.MoreHoriz
}

/**
 * The icon keys offered in the category-manager icon picker, in display order.
 * Every entry resolves through [iconForKey]; "other" maps to the dot fallback.
 */
val CategoryIconCatalog: List<String> = listOf(
    "food", "groceries", "coffee", "travel", "transit", "flight", "petrol",
    "shopping", "bills", "subscriptions", "phone", "home", "health", "fitness",
    "beauty", "entertainment", "music", "games", "sports", "education", "kids",
    "pets", "gift", "charity", "work", "investment", "savings", "other",
)

/**
 * The colour swatches offered in the category-manager colour picker, as stored
 * "#RRGGBB" strings. Mirrors CozyColors.categorySwatches plus Sports green and a
 * few extra earthy tones so custom categories still feel on-palette.
 */
val CategorySwatchHexes: List<String> = listOf(
    "#E0795B", // terracotta (Food)
    "#6FA8A0", // teal (Travel)
    "#8FB996", // sage (Investment)
    "#D9A441", // ochre (Petrol)
    "#B98BC9", // mauve (Shopping)
    "#7E97C9", // slate blue (Bills)
    "#E08B7B", // clay (Health)
    "#E8C06B", // honey (Entertainment)
    "#7FB069", // green (Sports)
    "#9C8C7A", // taupe (Other)
    "#C97B3C", // deep amber
    "#D98C8C", // dusty rose
)

/** Parse a stored "#RRGGBB" into a Compose Color, with a safe fallback. */
fun parseColor(hex: String, fallback: Color = Color(0xFF9C8C7A)): Color =
    runCatching { Color(hex.toColorInt()) }.getOrDefault(fallback)
