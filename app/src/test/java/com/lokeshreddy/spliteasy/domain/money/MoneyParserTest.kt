package com.lokeshreddy.spliteasy.domain.money

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyParserTest {

    private fun valid(paise: Long) = ParsedAmount.Valid(paise)

    @Test
    fun `whole rupees`() {
        assertEquals(valid(2_40_000), parseInrToPaise("2400"))
        assertEquals(valid(700), parseInrToPaise("007"))
    }

    @Test
    fun `commas and rupee sign are ignored`() {
        assertEquals(valid(2_40_000), parseInrToPaise("2,400"))
        assertEquals(valid(1_00_000_00), parseInrToPaise("₹1,00,000"))
    }

    @Test
    fun `one or two decimals`() {
        assertEquals(valid(2_40_050), parseInrToPaise("2400.5"))
        assertEquals(valid(2_40_050), parseInrToPaise("2400.50"))
        assertEquals(valid(1), parseInrToPaise("0.01"))
        assertEquals(valid(50), parseInrToPaise(".5"))
        assertEquals(valid(500), parseInrToPaise("5."))
    }

    @Test
    fun `surrounding spaces are trimmed`() {
        assertEquals(valid(700), parseInrToPaise(" 7 "))
    }

    @Test
    fun `blank text is empty`() {
        assertEquals(ParsedAmount.Empty, parseInrToPaise(""))
        assertEquals(ParsedAmount.Empty, parseInrToPaise("   "))
    }

    @Test
    fun `more than two decimals is rejected`() {
        assertEquals(ParsedAmount.TooManyDecimals, parseInrToPaise("2400.505"))
    }

    @Test
    fun `invalid text is rejected`() {
        listOf("abc", "-5", "+5", "1.2.3", ".", "₹", ",", "12 34", "1e3").forEach { text ->
            assertEquals("for \"$text\"", ParsedAmount.Invalid, parseInrToPaise(text))
        }
    }

    @Test
    fun `too many digits is rejected instead of overflowing`() {
        assertEquals(ParsedAmount.Invalid, parseInrToPaise("1234567890123456"))
        assertEquals(valid(123_456_789_012_345L * 100), parseInrToPaise("123456789012345"))
    }
}
