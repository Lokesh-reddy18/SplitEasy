package com.split.easy.domain.balance

import com.split.easy.domain.model.Settlement

/** How much a member can pay another right now (Q4, Q5, F4). */
object PayableAmounts {

    /**
     * The suggested payment from [fromMemberId] to [toMemberId], minus that member's payments
     * to them that are still waiting for confirmation. Never below zero; at zero, Pay is disabled.
     */
    fun payable(
        fromMemberId: String,
        toMemberId: String,
        suggestions: List<SuggestedPayment>,
        settlements: List<Settlement>,
    ): Long {
        val suggested = suggestions
            .filter { it.fromMemberId == fromMemberId && it.toMemberId == toMemberId }
            .sumOf { it.amountPaise }
        val pending = settlements
            .filter {
                it.isOpenPending && it.fromMemberId == fromMemberId &&
                    it.toMemberId == toMemberId
            }
            .sumOf { it.amountPaise }
        return (suggested - pending).coerceAtLeast(0L)
    }
}
