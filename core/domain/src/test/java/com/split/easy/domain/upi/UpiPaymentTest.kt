package com.split.easy.domain.upi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiPaymentTest {

    @Test
    fun `design example - pay Priya 600 for Office lunches`() {
        assertEquals(
            "upi://pay?pa=priya.s%40okicici&pn=Priya%20Sharma&am=600.00&tn=SplitEasy%20%C2%B7%20Office%20lunches&cu=INR",
            UpiPayment.buildUri(
                "priya.s@okicici",
                "Priya Sharma",
                60_000,
                "SplitEasy · Office lunches",
            ),
        )
    }

    @Test
    fun `amounts always have two decimals`() {
        assertTrue(UpiPayment.buildUri("a@ybl", "A", 1, "x").contains("am=0.01"))
        assertTrue(UpiPayment.buildUri("a@ybl", "A", 33_333, "x").contains("am=333.33"))
        assertTrue(UpiPayment.buildUri("a@ybl", "A", 1_00_000_00, "x").contains("am=100000.00"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero can't be paid`() {
        UpiPayment.buildUri("a@ybl", "A", 0, "x")
    }

    @Test
    fun `long notes are shortened`() {
        val uri = UpiPayment.buildUri("a@ybl", "A", 100, "n".repeat(80))
        assertTrue(uri.contains("tn=" + "n".repeat(50) + "&"))
    }

    @Test
    fun `success with a transaction id`() {
        assertEquals(
            UpiResult.Success("481022756631"),
            UpiPayment.parseResponse(
                "txnId=481022756631&responseCode=00&Status=SUCCESS&txnRef=abc",
            ),
        )
        assertEquals(
            UpiResult.Success("R1"),
            UpiPayment.parseResponse("STATUS=success&ApprovalRefNo=R1"),
        )
        assertEquals(UpiResult.Success(null), UpiPayment.parseResponse("Status=SUCCESS"))
    }

    @Test
    fun `failure, cancel and nothing back`() {
        assertEquals(UpiResult.Failure, UpiPayment.parseResponse("Status=FAILURE&responseCode=ZD"))
        assertEquals(UpiResult.Failure, UpiPayment.parseResponse("Status=SUBMITTED"))
        assertEquals(UpiResult.NoResponse, UpiPayment.parseResponse(null))
        assertEquals(UpiResult.NoResponse, UpiPayment.parseResponse(""))
        assertEquals(UpiResult.NoResponse, UpiPayment.parseResponse("garbage"))
    }

    @Test
    fun `UPI ID format`() {
        listOf("priya.s@okicici", "ananya.r@okaxis", "ravi_k-1@ybl", "9876543210@paytm").forEach {
            assertTrue(it, UpiPayment.isValidUpiId(it))
        }
        listOf("", "priya", "@okicici", "priya@", "pri ya@ok", "a@1bank", "x@y").forEach {
            assertFalse(it, UpiPayment.isValidUpiId(it))
        }
    }
}
