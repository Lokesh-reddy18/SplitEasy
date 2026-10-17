package com.split.easy.domain.model

/** The parts of an expense that affect balances. Money is in paise. */
data class Expense(
    val id: String,
    val paidByMemberId: String,
    val amountPaise: Long,
    /** Each participant's share, in member order. Shares add up to [amountPaise]. */
    val sharesPaise: Map<String, Long>,
    val isDeleted: Boolean = false,
)
