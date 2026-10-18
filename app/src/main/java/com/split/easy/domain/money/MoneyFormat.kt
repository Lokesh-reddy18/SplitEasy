package com.split.easy.domain.money

import kotlin.math.abs

private const val RUPEE_SIGN = "₹"

/** The designs use a real minus sign (U+2212), not a hyphen. */
private const val MINUS_SIGN = "\u2212"

/**
 * Formats paise as Indian rupees with lakh grouping.
 * Paise are shown only when they're not zero: 2,40,000 → "₹2,400", 33,333 → "₹333.33".
 * Negative amounts get a minus sign: −1,70,000 → "−₹1,700".
 */
fun formatInr(paise: Long): String {
    val sign = if (paise < 0) MINUS_SIGN else ""
    // Divide before taking abs() so Long.MIN_VALUE can't overflow.
    val rupees = abs(paise / PAISE_PER_RUPEE)
    val remainingPaise = abs(paise % PAISE_PER_RUPEE)
    val decimals = when (remainingPaise) {
        0L -> ""
        else -> "." + remainingPaise.toString().padStart(2, '0')
    }
    return sign + RUPEE_SIGN + groupIndian(rupees) + decimals
}

/**
 * Like [formatInr], but positive amounts get a "+" (for net balances).
 * "+₹2,000", "−₹1,700", "₹0".
 */
fun formatInrSigned(paise: Long): String = when {
    paise > 0 -> "+" + formatInr(paise)
    else -> formatInr(paise)
}

/**
 * Indian digit grouping: the last three digits, then groups of two.
 * 1234567 → "12,34,567". Written by hand so it's the same on every device and JVM.
 */
private fun groupIndian(number: Long): String = groupIndianDigits(number.toString())

/** The same grouping for a string of digits, used while typing an amount: "240000" → "2,40,000". */
fun groupIndianDigits(digits: String): String {
    if (digits.length <= 3) return digits
    val lastThree = digits.takeLast(3)
    val leading = digits.dropLast(3)
    val leadingGroups = leading.reversed().chunked(2).joinToString(",").reversed()
    return "$leadingGroups,$lastThree"
}

/**
 * Paise as plain text for an input field, with no rupee sign or commas: 2,40,000 → "2400",
 * 33,333 → "333.33", 2,40,050 → "2400.50". Used to prefill the amount when editing.
 */
fun formatPlainAmount(paise: Long): String {
    val rupees = paise / PAISE_PER_RUPEE
    val remainder = abs(paise % PAISE_PER_RUPEE)
    return if (remainder == 0L) "$rupees" else "$rupees." + remainder.toString().padStart(2, '0')
}
