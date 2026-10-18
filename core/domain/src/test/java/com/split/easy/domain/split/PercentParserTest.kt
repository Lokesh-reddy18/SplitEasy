package com.split.easy.domain.split

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PercentParserTest {

    @Test
    fun `percentages become basis points`() {
        assertEquals(3_333, parsePercentToBasisPoints("33.33"))
        assertEquals(5_000, parsePercentToBasisPoints("50"))
        assertEquals(5_050, parsePercentToBasisPoints("50.5"))
        assertEquals(10_000, parsePercentToBasisPoints("100"))
        assertEquals(0, parsePercentToBasisPoints("0"))
        assertEquals(1, parsePercentToBasisPoints(" 0.01 "))
    }

    @Test
    fun `invalid percentages are rejected`() {
        listOf("", "abc", "-5", "100.01", "250", "33.333", "₹50").forEach {
            assertNull("for \"$it\"", parsePercentToBasisPoints(it))
        }
    }

    @Test
    fun `basis points format back without trailing zeros`() {
        assertEquals("33.33", formatBasisPoints(3_333))
        assertEquals("50", formatBasisPoints(5_000))
        assertEquals("50.5", formatBasisPoints(5_050))
        assertEquals("0.01", formatBasisPoints(1))
        assertEquals("100", formatBasisPoints(10_000))
    }
}
