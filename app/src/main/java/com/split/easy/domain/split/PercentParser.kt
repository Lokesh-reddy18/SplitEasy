package com.split.easy.domain.split

import com.split.easy.domain.money.ParsedAmount
import com.split.easy.domain.money.parseInrToPaise

/**
 * Reads a typed percentage with up to two decimals as basis points: "33.33" → 3333, "50" → 5000.
 * It's the same "whole number with two decimals" shape as rupees and paise, so it reuses that parser
 * (and never uses Double). Returns null for anything that isn't a valid percentage up to 100.
 */
fun parsePercentToBasisPoints(text: String): Int? {
    if (text.trim().startsWith("₹")) return null
    val parsed = parseInrToPaise(text) as? ParsedAmount.Valid ?: return null
    return parsed.paise.takeIf { it <= FULL_PERCENT_BASIS_POINTS }?.toInt()
}

/** 3333 → "33.33", 5000 → "50". For showing stored percentages. */
fun formatBasisPoints(basisPoints: Int): String {
    val whole = basisPoints / 100
    val fraction = basisPoints % 100
    return if (fraction ==
        0
    ) {
        "$whole"
    } else {
        "$whole." + fraction.toString().padStart(2, '0').trimEnd('0')
    }
}
