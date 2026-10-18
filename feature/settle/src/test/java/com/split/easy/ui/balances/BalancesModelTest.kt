package com.split.easy.ui.balances

import com.split.easy.domain.balance.BalanceCalculator
import com.split.easy.domain.balance.CoorgTrip
import com.split.easy.domain.balance.SettlementSimplifier
import com.split.easy.domain.balance.rupees
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.testSettlement
import com.split.easy.domain.usecase.GroupBalances
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BalancesModelTest {

    private val members = CoorgTrip.members.mapIndexed { i, id ->
        Member(
            id,
            id.replaceFirstChar {
                it.uppercase()
            },
            userId = "uid-$id",
            createdAt = i.toLong(),
        )
    }

    private fun balances(
        me: String?,
        pending: List<com.split.easy.domain.model.Settlement> = CoorgTrip.settlements.filter {
            it.isOpenPending
        },
    ): GroupBalances {
        val net = BalanceCalculator.netBalances(
            CoorgTrip.members,
            CoorgTrip.expenses,
            CoorgTrip.settlements,
        )
        return GroupBalances(members, net, SettlementSimplifier.simplify(net), pending, me)
    }

    @Test
    fun `design 05 - the payee sees Ravi's payment to confirm and the four suggestions`() {
        val state = buildBalances("Coorg trip", isArchived = false, balances("you"))

        val confirm = state.toConfirm.single()
        assertEquals("Ravi", confirm.fromName)
        assertTrue(confirm.canAct)
        assertEquals(emptyList<PendingRow>(), state.waiting)

        assertEquals(
            listOf("Arjun→Meera", "Ravi→null", "Ravi→Priya", "Ravi→Meera"),
            state.suggestions.map { "${it.fromName}→${it.toName}" },
        )
        assertTrue(state.suggestions[1].hasPending)
        assertTrue(state.suggestions.none { it.isMine })

        // Net balances, biggest creditor first.
        assertEquals(
            listOf(
                "Meera" to rupees(2_000),
                null to rupees(1_200),
                "Priya" to rupees(300),
                "Ravi" to rupees(-1_700),
                "Arjun" to rupees(-1_800),
            ),
            state.net.map { it.name to it.netPaise },
        )
    }

    @Test
    fun `design 22 - Ravi sees his payment waiting, and Pay only on what's left`() {
        val state = buildBalances("Coorg trip", isArchived = false, balances("ravi"))

        assertEquals(emptyList<PendingRow>(), state.toConfirm)
        val waiting = state.waiting.single()
        assertEquals("You", waiting.toName)
        assertTrue(waiting.canAct)

        val mine = state.suggestions.filter { it.isMine }
        assertEquals(
            listOf("You" to 0L, "Priya" to rupees(300), "Meera" to rupees(200)),
            mine.map {
                it.toName to
                    it.payablePaise
            },
        )
        assertNull(mine.first().fromName)
    }

    @Test
    fun `archived groups and people who left can't act`() {
        val archived = buildBalances("Coorg trip", isArchived = true, balances("you"))
        assertTrue(archived.isReadOnly)
        assertFalse(archived.toConfirm.single().canAct)

        val left = buildBalances("Coorg trip", isArchived = false, balances(me = null))
        assertTrue(left.isReadOnly)
        assertEquals(emptyList<PendingRow>(), left.toConfirm)
    }

    @Test
    fun `a debt bigger than the pending payment can be paid for the extra`() {
        val small = testSettlement("ravi", "you", rupees(1_000), SettlementStatus.PENDING)
        val ravi = buildBalances("x", false, balances("ravi", listOf(small)))
        assertEquals(rupees(200), ravi.suggestions.first { it.toMemberId == "you" }.payablePaise)
    }

    @Test
    fun `UPI references are grouped in fours`() {
        assertEquals("4810 2275 6631", formatUpiRef("481022756631"))
        assertEquals("ABC", formatUpiRef("ABC"))
    }
}
