package com.lokeshreddy.spliteasy.domain.balance

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementSimplifierTest {

    @Test
    fun `design example - Coorg trip gives the four payments on the Balances screen`() {
        val net = BalanceCalculator.netBalances(
            CoorgTrip.members,
            CoorgTrip.expenses,
            CoorgTrip.settlements,
        )
        val expected = listOf(
            SuggestedPayment("arjun", "meera", rupees(1_800)),
            SuggestedPayment("ravi", "you", rupees(1_200)),
            SuggestedPayment("ravi", "priya", rupees(300)),
            SuggestedPayment("ravi", "meera", rupees(200)),
        )
        assertEquals(expected, SettlementSimplifier.simplify(net))
    }

    @Test
    fun `everyone settled means no payments`() {
        assertEquals(
            emptyList<SuggestedPayment>(),
            SettlementSimplifier.simplify(
                mapOf(
                    "a" to 0L,
                    "b" to 0L,
                ),
            ),
        )
        assertEquals(emptyList<SuggestedPayment>(), SettlementSimplifier.simplify(emptyMap()))
    }

    @Test
    fun `two people need one payment`() {
        val payments = SettlementSimplifier.simplify(mapOf("a" to 500L, "b" to -500L))
        assertEquals(listOf(SuggestedPayment("b", "a", 500)), payments)
    }

    @Test
    fun `equal amounts are ordered by member order`() {
        val payments = SettlementSimplifier.simplify(
            linkedMapOf("c" to 100L, "a" to 100L, "d" to -100L, "b" to -100L),
        )
        val expected = listOf(SuggestedPayment("d", "c", 100), SuggestedPayment("b", "a", 100))
        assertEquals(expected, payments)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `balances that don't add up to zero are rejected`() {
        SettlementSimplifier.simplify(mapOf("a" to 100L, "b" to -99L))
    }

    @Test
    fun `random groups - at most n minus 1 payments that settle everyone`() {
        val random = Random(seed = 7)
        repeat(1_000) {
            val net = randomBalances(random)
            val payments = SettlementSimplifier.simplify(net)

            val nonZero = net.values.count { it != 0L }
            assertTrue(payments.size <= maxOf(nonZero - 1, 0))
            assertTrue(payments.all { it.amountPaise > 0 && it.fromMemberId != it.toMemberId })

            val after = net.toMutableMap()
            payments.forEach {
                after[it.fromMemberId] = after.getValue(it.fromMemberId) + it.amountPaise
                after[it.toMemberId] = after.getValue(it.toMemberId) - it.amountPaise
            }
            assertTrue("not settled: $after", after.values.all { it == 0L })
        }
    }

    private fun randomBalances(random: Random): Map<String, Long> {
        val size = random.nextInt(1, 15)
        val values = MutableList(size) { random.nextLong(-1_000_000, 1_000_000) }
        values[size - 1] -= values.sum() // make the balances add up to zero
        return values.withIndex().associate { (index, value) -> "m$index" to value }
    }
}
