package com.split.easy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.split.easy.R

/** One bundled variable font file (OFL licence in assets/licenses) covers weights 400 to 800. */
@OptIn(ExperimentalTextApi::class) // variable-font weights; stable on API 26+, which is our minSdk
private fun plusJakartaSans(weight: Int) = Font(
    resId = R.font.plus_jakarta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val PlusJakartaSans = FontFamily(
    plusJakartaSans(400),
    plusJakartaSans(500),
    plusJakartaSans(600),
    plusJakartaSans(700),
    plusJakartaSans(800),
)

/** Digits of equal width, so amounts line up in lists ("tabular numbers" in the designs). */
const val TABULAR_NUMBERS = "tnum"

private fun style(size: Int, weight: Int, lineHeight: Int, letterSpacing: Double = 0.0) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/** Type scale from docs/designs/README.md, mapped onto Material 3's roles. */
val SplitEasyTypography = Typography(
    // Big amounts: "₹2,450" on the summary card, the amount field
    displaySmall = style(
        38,
        800,
        46,
        letterSpacing = -1.0,
    ).copy(fontFeatureSettings = TABULAR_NUMBERS),
    // Group balance: "You are owed ₹1,200"
    headlineSmall = style(28, 800, 36, letterSpacing = -0.5),
    // Screen titles: "Profile", "Insights"
    headlineMedium = style(26, 800, 34, letterSpacing = -0.5),
    // Sheet and dialog titles: "Settle up"
    titleLarge = style(20, 800, 28),
    // Top bar titles: "Coorg trip"
    titleMedium = style(18, 700, 24),
    // Section titles: "Your groups"
    titleSmall = style(17, 700, 24),
    // Row titles and main body text
    bodyLarge = style(15, 600, 22),
    // Longer explanations
    bodyMedium = style(14, 500, 21),
    // Captions and subtitles: "4 members"
    bodySmall = style(13, 500, 18),
    // Buttons
    labelLarge = style(14, 700, 20),
    // Section headers in capitals: "NEEDS YOUR CONFIRMATION"
    labelMedium = style(13, 700, 18, letterSpacing = 0.4),
    // Chips and small captions: "Pending", "you borrowed"
    labelSmall = style(12, 600, 16),
)
