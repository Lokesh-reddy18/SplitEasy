package com.split.easy.domain.usecase

import app.cash.turbine.test
import com.split.easy.domain.balance.SuggestedPayment
import com.split.easy.domain.balance.equalExpense
import com.split.easy.domain.balance.rupees
import com.split.easy.domain.balance.settlement
import com.split.easy.domain.model.CurrentUser
import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.ExpenseDraft
import com.split.easy.domain.model.GroupEvent
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.Settlement
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.repository.ExpenseRepository
import com.split.easy.domain.repository.MemberRepository
import com.split.easy.domain.repository.SettlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveGroupBalancesTest {

    private val members = MutableStateFlow(
        listOf(
            Member("you", "You", userId = "uid-you", createdAt = 1),
            Member("ravi", "Ravi", userId = "uid-ravi", createdAt = 2),
            Member("arjun", "Arjun", userId = null, createdAt = 3),
        ),
    )
    private val expenses = MutableStateFlow<List<Expense>>(emptyList())
    private val settlements = MutableStateFlow<List<Settlement>>(emptyList())

    private val useCase = ObserveGroupBalances(
        memberRepository = object : MemberRepository {
            override fun observeMembers(groupId: String) = members
            override suspend fun addGuest(groupId: String, displayName: String) = error("unused")
            override suspend fun removeMember(memberId: String) = error("unused")
            override suspend fun setGuestUpiId(
                guestMemberId: String,
                actorMemberId: String,
                upiId: String,
            ) = error("unused")
            override suspend fun setMyUpiId(upiId: String) = error("unused")
            override fun observeEvents(groupId: String): Flow<List<GroupEvent>> = emptyFlow()
        },
        expenseRepository = object : ExpenseRepository {
            override fun observeExpenses(groupId: String) = expenses
            override fun observeExpense(expenseId: String): Flow<Expense?> = emptyFlow()
            override suspend fun addExpense(draft: ExpenseDraft) = error("unused")
            override suspend fun updateExpense(expenseId: String, draft: ExpenseDraft) =
                error("unused")
            override suspend fun deleteExpense(expenseId: String) = error("unused")
        },
        settlementRepository = object : SettlementRepository {
            override fun observeSettlements(groupId: String) = settlements
            override suspend fun recordSettlement(
                groupId: String,
                fromMemberId: String,
                toMemberId: String,
                amountPaise: Long,
                method: SettlementMethod,
                status: SettlementStatus,
                upiTxnId: String?,
            ) = error("unused")
            override suspend fun setStatus(settlementId: String, status: SettlementStatus) =
                error("unused")
            override suspend fun deleteSettlement(settlementId: String) = error("unused")
            override fun observeWaitingForMyConfirmation(): Flow<Map<String, Int>> = emptyFlow()
        },
        currentUserProvider = object : CurrentUserProvider {
            override fun currentUser() = CurrentUser("uid-you", "You")
        },
    )

    @Test
    fun `balances update as expenses and payments change`() = runTest {
        useCase("g1").test {
            val empty = awaitItem()
            assertEquals("you", empty.myMemberId)
            assertEquals(0L, empty.myNetPaise)
            assertEquals(emptyList<SuggestedPayment>(), empty.suggestions)

            expenses.value =
                listOf(equalExpense("fuel", "you", 3_000, listOf("you", "ravi", "arjun")))
            val afterFuel = awaitItem()
            assertEquals(rupees(2_000), afterFuel.myNetPaise)
            assertEquals(rupees(1_000), afterFuel.payable("ravi", "you"))

            // Ravi's UPI payment is pending: balances stay, but he can't pay that amount twice (Q4).
            settlements.value =
                listOf(settlement("ravi", "you", 1_000, status = SettlementStatus.PENDING))
            val pending = awaitItem()
            assertEquals(rupees(2_000), pending.myNetPaise)
            assertEquals(0L, pending.payable("ravi", "you"))
            assertEquals(1, pending.pendingSettlements.size)

            settlements.value = listOf(settlement("ravi", "you", 1_000))
            val confirmed = awaitItem()
            assertEquals(rupees(1_000), confirmed.myNetPaise)
            assertEquals(
                listOf(SuggestedPayment("arjun", "you", rupees(1_000))),
                confirmed.suggestions,
            )
        }
    }

    @Test
    fun `myMemberId is null once you've left the group`() = runTest {
        members.value = members.value.map { if (it.id == "you") it.copy(isRemoved = true) else it }
        useCase("g1").test {
            assertEquals(null, awaitItem().myMemberId)
        }
    }
}
