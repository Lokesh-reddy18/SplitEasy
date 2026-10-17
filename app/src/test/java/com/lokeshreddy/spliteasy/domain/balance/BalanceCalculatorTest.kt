package com.lokeshreddy.spliteasy.domain.balance

import com.lokeshreddy.spliteasy.domain.model.Expense
import com.lokeshreddy.spliteasy.domain.model.SettlementStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceCalculatorTest {

    @Test
    fun `design example - Coorg trip net balances match the Balances screen`() {
        val net = BalanceCalculator.netBalances(
            CoorgTrip.members,
            CoorgTrip.expenses,
            CoorgTrip.settlements,
        )
        val expected = mapOf(
            "you" to rupees(1_200),
            "priya" to rupees(300),
            "meera" to rupees(2_000),
            "ravi" to rupees(-1_700),
            "arjun" to rupees(-1_800),
        )
        assertEquals(expected, net)
        assertEquals(0L, net.values.sum())
    }

    @Test
    fun `deleted expenses are ignored`() {
        val deleted = equalExpense("x", "a", 900, listOf("a", "b", "c")).copy(isDeleted = true)
        val net = BalanceCalculator.netBalances(listOf("a", "b", "c"), listOf(deleted), emptyList())
        assertEquals(mapOf("a" to 0L, "b" to 0L, "c" to 0L), net)
    }

    @Test
    fun `only confirmed settlements count`() {
        val expense = equalExpense("x", "a", 1_000, listOf("a", "b"))
        val settlements = listOf(
            settlement("b", "a", 100, status = SettlementStatus.PENDING),
            settlement("b", "a", 200, status = SettlementStatus.REJECTED),
            settlement("b", "a", 300, isDeleted = true),
            settlement("b", "a", 400),
        )
        val net = BalanceCalculator.netBalances(listOf("a", "b"), listOf(expense), settlements)
        assertEquals(mapOf("a" to rupees(100), "b" to rupees(-100)), net)
    }

    @Test
    fun `payer who isn't in the split is owed the whole amount`() {
        val expense =
            Expense(
                "x",
                paidByMemberId = "a",
                amountPaise = 1_000,
                sharesPaise = mapOf("b" to 1_000),
            )
        val net = BalanceCalculator.netBalances(listOf("a", "b"), listOf(expense), emptyList())
        assertEquals(mapOf("a" to 1_000L, "b" to -1_000L), net)
    }

    @Test
    fun `members missing from the list are still counted, at the end`() {
        val expense = equalExpense("x", "removed", 200, listOf("a", "removed"))
        val net = BalanceCalculator.netBalances(listOf("a"), listOf(expense), emptyList())
        assertEquals(listOf("a", "removed"), net.keys.toList())
        assertEquals(mapOf("a" to rupees(-100), "removed" to rupees(100)), net)
    }

    @Test
    fun `members with no activity have a zero balance`() {
        val net = BalanceCalculator.netBalances(listOf("a", "b"), emptyList(), emptyList())
        assertEquals(mapOf("a" to 0L, "b" to 0L), net)
    }
}
