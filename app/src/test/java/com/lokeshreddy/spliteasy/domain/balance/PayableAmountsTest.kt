package com.lokeshreddy.spliteasy.domain.balance

import com.lokeshreddy.spliteasy.domain.model.SettlementStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PayableAmountsTest {

    private val suggestions = listOf(SuggestedPayment("ravi", "you", rupees(1_200)))

    @Test
    fun `nothing pending - the full suggested amount can be paid`() {
        assertEquals(rupees(1_200), PayableAmounts.payable("ravi", "you", suggestions, emptyList()))
    }

    @Test
    fun `pending payment for the full amount disables Pay`() {
        val pending = listOf(settlement("ravi", "you", 1_200, status = SettlementStatus.PENDING))
        assertEquals(0L, PayableAmounts.payable("ravi", "you", suggestions, pending))
    }

    @Test
    fun `new debt on top of a pending payment - only the extra can be paid`() {
        val bigger = listOf(SuggestedPayment("ravi", "you", rupees(1_500)))
        val pending = listOf(settlement("ravi", "you", 1_200, status = SettlementStatus.PENDING))
        assertEquals(rupees(300), PayableAmounts.payable("ravi", "you", bigger, pending))
    }

    @Test
    fun `only open pending payments for the same pair are subtracted`() {
        val others = listOf(
            settlement("ravi", "you", 100, status = SettlementStatus.PENDING, isDeleted = true),
            settlement("ravi", "you", 200, status = SettlementStatus.REJECTED),
            settlement("ravi", "you", 300, status = SettlementStatus.CONFIRMED),
            settlement("ravi", "priya", 400, status = SettlementStatus.PENDING),
            settlement("you", "ravi", 500, status = SettlementStatus.PENDING),
        )
        assertEquals(rupees(1_200), PayableAmounts.payable("ravi", "you", suggestions, others))
    }

    @Test
    fun `a pair with no suggested payment can't be paid`() {
        val pending = listOf(settlement("arjun", "you", 50, status = SettlementStatus.PENDING))
        assertEquals(0L, PayableAmounts.payable("arjun", "you", suggestions, pending))
    }
}
