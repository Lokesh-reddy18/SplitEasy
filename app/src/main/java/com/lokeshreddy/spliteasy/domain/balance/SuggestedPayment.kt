package com.lokeshreddy.spliteasy.domain.balance

/** One payment in the simplified "who owes whom" list. */
data class SuggestedPayment(
    val fromMemberId: String,
    val toMemberId: String,
    val amountPaise: Long,
)
