package com.split.easy.ui.groupdetail

import com.split.easy.domain.balance.SuggestedPayment
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.testSettlement
import com.split.easy.domain.usecase.GroupBalances
import org.junit.Assert.assertEquals
import org.junit.Test

class SettleUpTargetTest {

    private fun balances(
        suggestions: List<SuggestedPayment>,
        pending: List<com.split.easy.domain.model.Settlement> = emptyList(),
        me: String? = "you",
    ) = GroupBalances(
        members = listOf(Member("you", "You", "uid-you")),
        netBalances = emptyMap(),
        suggestions = suggestions,
        pendingSettlements = pending,
        myMemberId = me,
    )

    @Test
    fun `owing exactly one person opens the payment sheet for them`() {
        val target =
            settleUpTarget(
                balances(
                    listOf(
                        SuggestedPayment("you", "priya", 600_00),
                        SuggestedPayment("ravi", "meera", 1),
                    ),
                ),
            )
        assertEquals(SettleUpTarget.Pay("priya"), target)
    }

    @Test
    fun `owing several people or nobody opens Balances`() {
        val several =
            listOf(SuggestedPayment("you", "priya", 300), SuggestedPayment("you", "meera", 200))
        assertEquals(SettleUpTarget.Balances, settleUpTarget(balances(several)))
        assertEquals(
            SettleUpTarget.Balances,
            settleUpTarget(balances(listOf(SuggestedPayment("ravi", "you", 1_200)))),
        )
        assertEquals(SettleUpTarget.Balances, settleUpTarget(balances(emptyList(), me = null)))
    }

    @Test
    fun `a debt already covered by a pending payment doesn't count`() {
        val suggestions =
            listOf(SuggestedPayment("you", "priya", 300), SuggestedPayment("you", "meera", 200))
        val pending = listOf(testSettlement("you", "meera", 200, SettlementStatus.PENDING))
        assertEquals(SettleUpTarget.Pay("priya"), settleUpTarget(balances(suggestions, pending)))
    }
}
