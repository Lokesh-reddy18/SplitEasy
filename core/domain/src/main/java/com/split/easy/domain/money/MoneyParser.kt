package com.split.easy.domain.money

private val AMOUNT_PATTERN = Regex("""^(\d*)(?:\.(\d*))?$""")

/** More digits than this can't be a real amount and could overflow a Long. */
private const val MAX_RUPEE_DIGITS = 15

/**
 * Turns typed text such as "2,400", "333.33" or ".5" into paise.
 * It works on the digits as text, so no Float or Double is ever involved.
 * An optional leading "₹" and commas are allowed; signs and spaces inside the number are not.
 * This only parses; the amount limits are checked by the expense and settlement rules.
 */
fun parseInrToPaise(text: String): ParsedAmount {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return ParsedAmount.Empty

    val cleaned = trimmed.removePrefix("₹").trim().replace(",", "")
    val match = AMOUNT_PATTERN.matchEntire(cleaned) ?: return ParsedAmount.Invalid
    val rupeeDigits = match.groupValues[1]
    val paiseDigits = match.groupValues[2]

    return when {
        rupeeDigits.isEmpty() && paiseDigits.isEmpty() -> ParsedAmount.Invalid

        paiseDigits.length > 2 -> ParsedAmount.TooManyDecimals

        rupeeDigits.length > MAX_RUPEE_DIGITS -> ParsedAmount.Invalid

        else -> {
            val rupees = rupeeDigits.ifEmpty { "0" }.toLong()
            val paise = paiseDigits.padEnd(2, '0').toLong()
            ParsedAmount.Valid(rupees * PAISE_PER_RUPEE + paise)
        }
    }
}
