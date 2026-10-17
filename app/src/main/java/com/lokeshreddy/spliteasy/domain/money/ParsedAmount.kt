package com.lokeshreddy.spliteasy.domain.money

/** Result of reading an amount the user typed. */
sealed interface ParsedAmount {
    data class Valid(val paise: Long) : ParsedAmount

    /** Nothing typed yet. */
    data object Empty : ParsedAmount

    /** Not a number, negative, or too many digits. */
    data object Invalid : ParsedAmount

    /** More than two digits after the decimal point. */
    data object TooManyDecimals : ParsedAmount
}
