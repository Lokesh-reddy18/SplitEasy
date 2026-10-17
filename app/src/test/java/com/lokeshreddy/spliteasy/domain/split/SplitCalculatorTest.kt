package com.lokeshreddy.spliteasy.domain.split

import com.lokeshreddy.spliteasy.domain.money.MAX_AMOUNT_PAISE
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitCalculatorTest {

    private val coorgTrip = listOf("you", "priya", "meera", "ravi", "arjun")

    private fun shares(result: SplitResult): Map<String, Long> {
        assertTrue("expected success but was $result", result is SplitResult.Success)
        return (result as SplitResult.Success).sharesPaise
    }

    private fun error(result: SplitResult): SplitError {
        assertTrue("expected an error but was $result", result is SplitResult.Invalid)
        return (result as SplitResult.Invalid).error
    }

    // Equal splits

    @Test
    fun `design example - 2,400 split equally five ways is 480 each`() {
        val result = SplitCalculator.equal(2_40_000, coorgTrip, payerId = "priya")
        assertEquals(coorgTrip.associateWith { 48_000L }, shares(result))
    }

    @Test
    fun `100 rupees three ways gives the extra paisa to the payer`() {
        val result = SplitCalculator.equal(10_000, listOf("a", "b", "c"), payerId = "b")
        assertEquals(mapOf("a" to 3_333L, "b" to 3_334L, "c" to 3_333L), shares(result))
    }

    @Test
    fun `payer outside the split - first participant in member order gets the extra paisa`() {
        val result = SplitCalculator.equal(10_000, listOf("a", "b", "c"), payerId = "z")
        assertEquals(mapOf("a" to 3_334L, "b" to 3_333L, "c" to 3_333L), shares(result))
    }

    @Test
    fun `one paisa three ways goes entirely to the payer`() {
        val result = SplitCalculator.equal(1, listOf("a", "b", "c"), payerId = "c")
        assertEquals(mapOf("a" to 0L, "b" to 0L, "c" to 1L), shares(result))
    }

    @Test
    fun `all leftover paise go to the payer, not one each`() {
        // 100 paise / 7 = 14 each, 2 left over.
        val result = SplitCalculator.equal(100, listOf("a", "b", "c", "d", "e", "f", "g"), "a")
        val expected =
            mapOf("a" to 16L) + listOf("b", "c", "d", "e", "f", "g").associateWith { 14L }
        assertEquals(expected, shares(result))
    }

    @Test
    fun `single participant takes the whole amount`() {
        assertEquals(mapOf("a" to 999L), shares(SplitCalculator.equal(999, listOf("a"), "b")))
    }

    @Test
    fun `shares keep member order`() {
        val result = SplitCalculator.equal(300, listOf("c", "a", "b"), "a")
        assertEquals(listOf("c", "a", "b"), shares(result).keys.toList())
    }

    @Test
    fun `equal split rejects bad input`() {
        assertEquals(SplitError.NoParticipants, error(SplitCalculator.equal(100, emptyList(), "a")))
        assertEquals(
            SplitError.DuplicateParticipant,
            error(SplitCalculator.equal(100, listOf("a", "a"), "a")),
        )
    }

    // Totals

    @Test
    fun `total must be positive and within the cap`() {
        assertEquals(SplitError.TotalNotPositive, error(SplitCalculator.equal(0, listOf("a"), "a")))
        assertEquals(
            SplitError.TotalNotPositive,
            error(SplitCalculator.exact(-5, mapOf("a" to -5))),
        )
        assertEquals(
            SplitError.TotalAboveLimit,
            error(SplitCalculator.percent(MAX_AMOUNT_PAISE + 1, mapOf("a" to 10_000), "a")),
        )
        assertEquals(
            mapOf("a" to MAX_AMOUNT_PAISE),
            shares(SplitCalculator.equal(MAX_AMOUNT_PAISE, listOf("a"), "a")),
        )
    }

    // Percentage splits

    @Test
    fun `percentages that divide evenly need no rounding`() {
        val result = SplitCalculator.percent(
            10_000,
            mapOf("a" to 3_333, "b" to 3_333, "c" to 3_334),
            payerId = "a",
        )
        assertEquals(mapOf("a" to 3_333L, "b" to 3_333L, "c" to 3_334L), shares(result))
    }

    @Test
    fun `percentage rounding remainder goes to the payer`() {
        // 50% of 3 paise = 1.5 each, rounded down to 1, so 1 paisa is left over.
        val result = SplitCalculator.percent(3, mapOf("a" to 5_000, "b" to 5_000), payerId = "b")
        assertEquals(mapOf("a" to 1L, "b" to 2L), shares(result))
    }

    @Test
    fun `percentage remainder skips a payer who isn't in the split`() {
        val result = SplitCalculator.percent(
            1,
            mapOf("a" to 0, "b" to 5_000, "c" to 5_000),
            payerId = "z",
        )
        // "a" has 0%, so the first real participant is "b".
        assertEquals(mapOf("a" to 0L, "b" to 1L, "c" to 0L), shares(result))
    }

    @Test
    fun `payer with zero percent doesn't receive the remainder`() {
        val result = SplitCalculator.percent(3, mapOf("a" to 0, "b" to 5_000, "c" to 5_000), "a")
        assertEquals(mapOf("a" to 0L, "b" to 2L, "c" to 1L), shares(result))
    }

    @Test
    fun `percentages must add up to exactly 100`() {
        val short = SplitCalculator.percent(
            10_000,
            mapOf("a" to 3_333, "b" to 3_333, "c" to 3_333),
            "a",
        )
        assertEquals(SplitError.PercentSumMismatch(1), error(short))

        val over = SplitCalculator.percent(10_000, mapOf("a" to 6_000, "b" to 5_000), "a")
        assertEquals(SplitError.PercentSumMismatch(-1_000), error(over))
    }

    @Test
    fun `percentage split rejects negative or missing participants`() {
        assertEquals(
            SplitError.NegativeShare,
            error(SplitCalculator.percent(100, mapOf("a" to 11_000, "b" to -1_000), "a")),
        )
        assertEquals(
            SplitError.NoParticipants,
            error(SplitCalculator.percent(100, emptyMap(), "a")),
        )
    }

    // Exact splits

    @Test
    fun `exact amounts that add up are kept as they are`() {
        val amounts = mapOf("a" to 1_00_000L, "b" to 40_000L, "c" to 0L)
        assertEquals(amounts, shares(SplitCalculator.exact(1_40_000, amounts)))
    }

    @Test
    fun `exact amounts report what's left to assign or how much is over`() {
        assertEquals(
            SplitError.ExactSumMismatch(500),
            error(SplitCalculator.exact(2_000, mapOf("a" to 1_000, "b" to 500))),
        )
        assertEquals(
            SplitError.ExactSumMismatch(-500),
            error(SplitCalculator.exact(2_000, mapOf("a" to 1_500, "b" to 1_000))),
        )
    }

    @Test
    fun `exact split rejects bad amounts`() {
        assertEquals(
            SplitError.NegativeShare,
            error(SplitCalculator.exact(100, mapOf("a" to 150, "b" to -50))),
        )
        assertEquals(
            SplitError.ShareAboveTotal,
            error(SplitCalculator.exact(100, mapOf("a" to Long.MAX_VALUE, "b" to 1))),
        )
        assertEquals(SplitError.NoParticipants, error(SplitCalculator.exact(100, emptyMap())))
    }

    // Properties over many random inputs

    @Test
    fun `random equal and percent splits always add up to the total`() {
        val random = Random(seed = 42)
        repeat(1_000) {
            val people = (1..random.nextInt(1, 12)).map { "m$it" }
            val total = random.nextLong(1, MAX_AMOUNT_PAISE + 1)
            val payer = if (random.nextBoolean()) people.random(random) else "outsider"

            val equal = shares(SplitCalculator.equal(total, people, payer))
            assertEquals(total, equal.values.sum())
            assertTrue(equal.values.all { it >= 0 })
            assertTrue(equal.values.max() - equal.values.min() < people.size)

            val percent =
                shares(SplitCalculator.percent(total, randomPercentages(people, random), payer))
            assertEquals(total, percent.values.sum())
            assertTrue(percent.values.all { it >= 0 })
        }
    }

    private fun randomPercentages(people: List<String>, random: Random): Map<String, Int> {
        var left = FULL_PERCENT_BASIS_POINTS
        return people.mapIndexed { index, person ->
            val value = if (index == people.lastIndex) left else random.nextInt(0, left + 1)
            left -= value
            person to value
        }.toMap()
    }
}
