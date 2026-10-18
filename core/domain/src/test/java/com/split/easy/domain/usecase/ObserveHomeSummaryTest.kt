package com.split.easy.domain.usecase

import app.cash.turbine.test
import com.split.easy.domain.balance.equalExpense
import com.split.easy.domain.balance.rupees
import com.split.easy.domain.model.Group
import com.split.easy.domain.model.Member
import com.split.easy.testing.FakeCurrentUser
import com.split.easy.testing.FakeExpenseRepository
import com.split.easy.testing.FakeGroupRepository
import com.split.easy.testing.FakeMemberRepository
import com.split.easy.testing.FakeSettlementRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveHomeSummaryTest {

    private val groups = FakeGroupRepository()
    private val members = FakeMemberRepository()
    private val expenses = FakeExpenseRepository()
    private val settlements = FakeSettlementRepository()
    private val useCase = ObserveHomeSummary(
        groups,
        settlements,
        ObserveGroupBalances(members, expenses, settlements, FakeCurrentUser()),
    )

    private fun member(id: String, userId: String? = null, removed: Boolean = false) =
        Member(id, id, userId, isRemoved = removed)

    @Test
    fun `no groups gives an empty, settled summary`() = runTest {
        useCase().test {
            val summary = awaitItem()
            assertEquals(emptyList<GroupSummary>(), summary.groups)
            assertEquals(0L, summary.netPaise)
        }
    }

    @Test
    fun `totals add up what you owe and are owed across active groups`() = runTest {
        groups.groups.value = listOf(
            Group("flat", "Flat 4B", "uid-you", lastActivityAt = 3),
            Group("lunch", "Office lunches", "uid-you", lastActivityAt = 2),
            Group("home", "Home", "uid-you", lastActivityAt = 1),
            Group("old", "Goa trip", "uid-you", isArchived = true),
        )
        members.members.value = mapOf(
            "flat" to listOf(
                member("me-f", "uid-you"),
                member("a"),
                member("b"),
                member("gone", removed = true),
            ),
            "lunch" to listOf(member("me-l", "uid-you"), member("c")),
            "home" to listOf(member("me-h", "uid-you"), member("d")),
            "old" to listOf(member("me-o", "uid-you"), member("e")),
        )
        // You're owed 2,000 in the flat, owe 600 for lunch; the archived trip is ignored.
        expenses.expenses.value = mapOf(
            "flat" to listOf(equalExpense("rent", "me-f", 3_000, listOf("me-f", "a", "b"))),
            "lunch" to listOf(equalExpense("thali", "c", 1_200, listOf("me-l", "c"))),
            "old" to listOf(equalExpense("hotel", "me-o", 9_000, listOf("me-o", "e"))),
        )
        settlements.waiting.value = mapOf("flat" to 1)

        useCase().test {
            val summary = awaitItem()
            assertEquals(listOf("flat", "lunch", "home"), summary.groups.map { it.group.id })
            assertEquals(
                listOf(rupees(2_000), rupees(-600), 0L),
                summary.groups.map {
                    it.myNetPaise
                },
            )
            assertEquals(listOf(3, 2, 2), summary.groups.map { it.memberCount })
            assertEquals(listOf(1, 0, 0), summary.groups.map { it.waitingForMe })
            assertEquals(rupees(600), summary.owePaise)
            assertEquals(rupees(2_000), summary.owedPaise)
            assertEquals(rupees(1_400), summary.netPaise)
        }
    }
}
