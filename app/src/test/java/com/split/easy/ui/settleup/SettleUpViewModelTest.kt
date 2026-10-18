package com.split.easy.ui.settleup

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.split.easy.domain.balance.equalExpense
import com.split.easy.domain.balance.rupees
import com.split.easy.domain.model.Group
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.settlement.AmountCheck
import com.split.easy.domain.testSettlement
import com.split.easy.domain.usecase.ObserveGroupBalances
import com.split.easy.testing.FakeCurrentUser
import com.split.easy.testing.FakeExpenseRepository
import com.split.easy.testing.FakeGroupRepository
import com.split.easy.testing.FakeMemberRepository
import com.split.easy.testing.FakeSettlementRepository
import com.split.easy.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettleUpViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val groups = FakeGroupRepository().apply {
        groups.value =
            listOf(Group("g1", "Office lunches", "uid-you"))
    }
    private val members = FakeMemberRepository().apply {
        members.value = mapOf(
            "g1" to listOf(
                Member("you", "You", "uid-you", createdAt = 1),
                Member(
                    "priya",
                    "Priya Sharma",
                    "uid-priya",
                    upiId = "priya.s@okicici",
                    createdAt = 2,
                ),
                Member("karan", "Karan", "uid-karan", createdAt = 3),
                Member("arjun", "Arjun", null, createdAt = 4),
            ),
        )
    }

    // You owe Priya ₹600, Karan ₹450 and Arjun ₹300.
    private val expenses = FakeExpenseRepository().apply {
        expenses.value = mapOf(
            "g1" to listOf(
                equalExpense("thali", "priya", 1_200, listOf("you", "priya")),
                equalExpense("chai", "karan", 900, listOf("you", "karan")),
                equalExpense("dosa", "arjun", 600, listOf("you", "arjun")),
            ),
        )
    }
    private val settlements = FakeSettlementRepository()

    private fun viewModel(to: String) = SettleUpViewModel(
        SavedStateHandle(mapOf("groupId" to "g1", "toMemberId" to to)),
        groups,
        members,
        settlements,
        ObserveGroupBalances(members, expenses, settlements, FakeCurrentUser()),
    )

    @Test
    fun `design 06 - starts at the full balance with the group in the note`() {
        val state = viewModel("priya").uiState.value
        assertEquals(rupees(600), state.payablePaise)
        assertEquals("600", state.amountText)
        assertEquals("SplitEasy · Office lunches", state.note)
        assertTrue(state.canPayWithUpi)
    }

    @Test
    fun `the UPI link has the payee, amount and note`() {
        val vm = viewModel("priya")
        vm.onAmountChange("250.5")
        assertEquals(
            "upi://pay?pa=priya.s%40okicici&pn=Priya%20Sharma&am=250.50&tn=SplitEasy%20%C2%B7%20Office%20lunches&cu=INR",
            vm.upiUri(),
        )
    }

    @Test
    fun `overpaying is blocked`() {
        val vm = viewModel("priya")
        vm.onAmountChange("600.01")
        assertEquals(AmountCheck.ABOVE_PAYABLE, vm.uiState.value.amountCheck)
        assertFalse(vm.uiState.value.canPayWithUpi)
        assertNull(vm.upiUri())
    }

    @Test
    fun `UPI success records a pending payment with its transaction id`() = runTest {
        val vm = viewModel("priya")
        vm.doneEvents.test {
            vm.onUpiResult("txnId=481022756631&responseCode=00&Status=SUCCESS")
            awaitItem()
        }
        val saved = settlements.recorded.single()
        assertEquals(SettlementMethod.UPI, saved.method)
        assertEquals(SettlementStatus.PENDING, saved.status)
        assertEquals("481022756631", saved.upiTxnId)
        assertEquals(rupees(600), saved.amountPaise)
    }

    @Test
    fun `failure or no response records nothing and offers Record payment`() {
        val vm = viewModel("priya")
        vm.onUpiResult("Status=FAILURE")
        assertEquals(PayOutcome.NOT_COMPLETED, vm.uiState.value.outcome)
        vm.onUpiResult(null)
        assertEquals(PayOutcome.NOT_COMPLETED, vm.uiState.value.outcome)
        assertEquals(emptyList<Any>(), settlements.recorded)
    }

    @Test
    fun `cash to a real user is pending, to a guest it's confirmed`() = runTest {
        val toPriya = viewModel("priya")
        toPriya.doneEvents.test {
            toPriya.onRecordCash()
            awaitItem()
        }
        val toArjun = viewModel("arjun")
        toArjun.doneEvents.test {
            toArjun.onRecordCash()
            awaitItem()
        }
        assertEquals(
            listOf(
                SettlementMethod.CASH to SettlementStatus.PENDING,
                SettlementMethod.CASH to SettlementStatus.CONFIRMED,
            ),
            settlements.recorded.map { it.method to it.status },
        )
    }

    @Test
    fun `design 21 - a payee with no UPI ID can't be paid by UPI, but cash still works`() {
        val state = viewModel("karan").uiState.value
        assertNull(state.payee!!.upiId)
        assertFalse(state.canPayWithUpi)
        assertTrue(state.canRecord)
    }

    @Test
    fun `a guest's UPI ID can be added here, logged as a change by you`() {
        val vm = viewModel("arjun")
        vm.onSetGuestUpiId("arjun@oksbi")
        assertEquals(listOf(Triple("arjun", "you", "arjun@oksbi")), members.guestUpiChanges)
        assertEquals("arjun@oksbi", vm.uiState.value.payee!!.upiId)
        assertTrue(vm.uiState.value.canPayWithUpi)
    }

    @Test
    fun `a pending payment lowers what can be paid`() {
        settlements.settlements.value = mapOf(
            "g1" to listOf(testSettlement("you", "priya", rupees(400), SettlementStatus.PENDING)),
        )
        assertEquals(rupees(200), viewModel("priya").uiState.value.payablePaise)
    }
}
