package com.split.easy.domain.money

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatTest {

    @Test
    fun `zero shows no paise`() {
        assertEquals("₹0", formatInr(0))
    }

    @Test
    fun `paise below one rupee keep two digits`() {
        assertEquals("₹0.05", formatInr(5))
        assertEquals("₹0.50", formatInr(50))
    }

    @Test
    fun `whole rupees hide the paise`() {
        assertEquals("₹480", formatInr(48_000))
        assertEquals("₹2,400", formatInr(2_40_000))
    }

    @Test
    fun `non-zero paise are shown`() {
        assertEquals("₹333.33", formatInr(33_333))
        assertEquals("₹2,400.50", formatInr(2_40_050))
    }

    @Test
    fun `uses Indian lakh and crore grouping`() {
        assertEquals("₹1,000", formatInr(1_000_00))
        assertEquals("₹99,999", formatInr(99_999_00))
        assertEquals("₹1,00,000", formatInr(1_00_000_00))
        assertEquals("₹12,34,567", formatInr(12_34_567_00))
        assertEquals("₹1,00,00,000", formatInr(1_00_00_000_00))
    }

    @Test
    fun `the amount cap formats as ten lakh`() {
        assertEquals("₹10,00,000", formatInr(MAX_AMOUNT_PAISE))
    }

    @Test
    fun `negative amounts use a real minus sign`() {
        assertEquals("\u2212₹1,700", formatInr(-1_70_000))
        assertEquals("\u2212₹0.01", formatInr(-1))
    }

    @Test
    fun `extreme values do not overflow`() {
        assertEquals("\u2212₹92,23,37,20,36,85,47,758.08", formatInr(Long.MIN_VALUE))
        assertEquals("₹92,23,37,20,36,85,47,758.07", formatInr(Long.MAX_VALUE))
    }

    @Test
    fun `signed format adds plus to positive amounts only`() {
        assertEquals("+₹2,000", formatInrSigned(2_00_000))
        assertEquals("\u2212₹1,700", formatInrSigned(-1_70_000))
        assertEquals("₹0", formatInrSigned(0))
    }
}
