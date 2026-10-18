package com.split.easy.domain.money

/** Money is always stored as a whole number of paise. */
const val PAISE_PER_RUPEE: Long = 100L

/**
 * Largest amount allowed for a single expense or settlement: ₹10,00,000.
 * It catches typos such as an extra zero.
 */
const val MAX_AMOUNT_PAISE: Long = 1_000_000L * PAISE_PER_RUPEE
