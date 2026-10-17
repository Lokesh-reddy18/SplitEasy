package com.split.easy.domain.expense

/** Result of checking an expense's amount and date before saving. */
enum class ExpenseCheck {
    OK,
    AMOUNT_NOT_POSITIVE,
    AMOUNT_ABOVE_LIMIT,

    /** More than 30 days ahead. */
    DATE_TOO_FAR_AHEAD,
}
