package com.balance.budget.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.balance.budget.R

/**
 * Type system. Two characterful downloadable families, fetched once via the
 * Google Fonts provider and cached by the system:
 *   - Display ("Bricolage Grotesque") — the hero amounts and big numbers.
 *   - Body ("Plus Jakarta Sans") — everything else, clean and readable.
 *
 * If the provider is unavailable, GoogleFont falls back to the system font
 * automatically, so the app always renders.
 */
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val displayGF = GoogleFont("Bricolage Grotesque")
private val bodyGF = GoogleFont("Plus Jakarta Sans")

val DisplayFamily = FontFamily(
    Font(googleFont = displayGF, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = displayGF, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = displayGF, fontProvider = provider, weight = FontWeight.ExtraBold),
)

val BodyFamily = FontFamily(
    Font(googleFont = bodyGF, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = bodyGF, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = bodyGF, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = bodyGF, fontProvider = provider, weight = FontWeight.Bold),
)

/** Extra-large styles for the hero amount (not part of M3 Typography). */
val HeroAmountStyle = TextStyle(
    fontFamily = DisplayFamily,
    fontWeight = FontWeight.ExtraBold,
    fontStyle = FontStyle.Normal,
    fontSize = 56.sp,
    lineHeight = 60.sp,
    letterSpacing = (-1.5).sp,
)

val BigAmountStyle = TextStyle(
    fontFamily = DisplayFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.5).sp,
)

val BalanceTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 45.sp, lineHeight = 50.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)
