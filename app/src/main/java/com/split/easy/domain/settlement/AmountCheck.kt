package com.split.easy.domain.settlement

/** Result of checking a settlement amount the user entered. */
enum class AmountCheck {
    OK,
    NOT_POSITIVE,

    /** More than the payer currently owes this person (overpaying is blocked, Q5). */
    ABOVE_PAYABLE,

    /** More than ₹10,00,000. */
    ABOVE_LIMIT,
}
