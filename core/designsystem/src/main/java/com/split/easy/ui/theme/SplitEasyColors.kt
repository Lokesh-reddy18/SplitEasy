package com.split.easy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The design tokens from docs/designs/README.md. Material 3's colour scheme has no slots for
 * "you owe" or the dark summary card, so the full set lives here and the scheme is derived from it.
 */
@Immutable
data class SplitEasyColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val border: Color,
    val divider: Color,
    // Violet: buttons, and "you are owed"
    val primary: Color,
    val onPrimary: Color,
    val primaryText: Color,
    val primaryTint: Color,
    val onPrimaryTint: Color,
    // Amber: "you owe", destructive actions, offline banner
    val owe: Color,
    val onOwe: Color,
    val oweTint: Color,
    val onOweTint: Color,
    // Teal: secondary tiles, Travel
    val secondaryTint: Color,
    val onSecondaryTint: Color,
    // Grey tiles and the segmented control
    val neutralTint: Color,
    val onNeutralTint: Color,
    // The dark "Overall" summary card on Groups home
    val summaryCard: Color,
    val summaryTile: Color,
    val onSummary: Color,
    val summaryMuted: Color,
    val summaryOwed: Color,
    val summaryOwe: Color,
)

val LightColors = SplitEasyColors(
    isDark = false,
    background = Color(0xFFF6F5FB),
    surface = Color(0xFFFFFFFF),
    ink = Color(0xFF17142B),
    muted = Color(0xFF5E5A73),
    border = Color(0xFFE6E3EF),
    divider = Color(0xFFF0EEF5),
    primary = Color(0xFF5B3FD6),
    onPrimary = Color(0xFFFFFFFF),
    primaryText = Color(0xFF5B3FD6),
    primaryTint = Color(0xFFECE8FD),
    onPrimaryTint = Color(0xFF4329B0),
    owe = Color(0xFFB45309),
    onOwe = Color(0xFFFFFFFF),
    oweTint = Color(0xFFFEF1E2),
    onOweTint = Color(0xFF8A4006),
    secondaryTint = Color(0xFFE0F2F5),
    onSecondaryTint = Color(0xFF0E7490),
    neutralTint = Color(0xFFF0EEF5),
    onNeutralTint = Color(0xFF403B55),
    summaryCard = Color(0xFF17142B),
    summaryTile = Color(0xFF2B2650),
    onSummary = Color(0xFFFFFFFF),
    summaryMuted = Color(0xFFC9C4E6),
    summaryOwed = Color(0xFFB9A9FF),
    summaryOwe = Color(0xFFFCD29F),
)

/** Dark palette approved in Q19. Every text pair is at least 4.5:1 contrast. */
val DarkColors = SplitEasyColors(
    isDark = true,
    background = Color(0xFF100E1C),
    surface = Color(0xFF1B1830),
    ink = Color(0xFFF2F0FA),
    muted = Color(0xFFA9A4C2),
    border = Color(0xFF2E2A47),
    divider = Color(0xFF25213A),
    primary = Color(0xFF6D52E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryText = Color(0xFFB9A9FF),
    primaryTint = Color(0xFF2E2858),
    onPrimaryTint = Color(0xFFCFC4FF),
    owe = Color(0xFFFCC48A),
    onOwe = Color(0xFF17142B),
    oweTint = Color(0xFF3A2817),
    onOweTint = Color(0xFFFCD29F),
    secondaryTint = Color(0xFF10303A),
    onSecondaryTint = Color(0xFF7DD3E3),
    neutralTint = Color(0xFF25213A),
    onNeutralTint = Color(0xFFC9C4E6),
    summaryCard = Color(0xFF262140),
    summaryTile = Color(0xFF322B55),
    onSummary = Color(0xFFFFFFFF),
    summaryMuted = Color(0xFFC9C4E6),
    summaryOwed = Color(0xFFB9A9FF),
    summaryOwe = Color(0xFFFCD29F),
)
