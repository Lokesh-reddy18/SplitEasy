package com.split.easy.ui.settleup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.split.easy.domain.model.Member
import com.split.easy.ui.theme.SplitEasyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w390dp-h844dp")
class SettleUpContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val me = Member("you", "You", "uid-you")
    private val priya = Member("priya", "Priya Sharma", "uid-priya", upiId = "priya.s@okicici")

    private fun state(payee: Member, outcome: PayOutcome = PayOutcome.NONE) = SettleUpUiState(
        isLoading = false,
        groupName = "Office lunches",
        payee = payee,
        me = me,
        payablePaise = 60_000,
        amountText = "600",
        note = "SplitEasy · Office lunches",
        outcome = outcome,
    )

    private val calls = mutableListOf<String>()

    private fun show(state: SettleUpUiState) = composeRule.setContent {
        SplitEasyTheme {
            SettleUpContent(state, {}, {}, onPay = { calls += "pay" }, onRecordCash = {
                calls +=
                    "cash"
            }, onAddGuestUpi = { calls += "guest" })
        }
    }

    @Test
    fun `design 06 - pay Priya with the UPI app`() {
        show(state(priya))
        composeRule.onNodeWithText("priya.s@okicici").assertIsDisplayed()
        composeRule.onNodeWithText("Full balance. You can pay part of it.").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Opens GPay, PhonePe, Paytm or another UPI app. If it reports success, Priya Sharma is asked to confirm.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Pay ₹600 with UPI app").performClick()
        composeRule.onNodeWithText("Paid in cash? Record payment").performClick()
        assertEquals(listOf("pay", "cash"), calls)
    }

    @Test
    fun `design 21 - no UPI ID disables Pay`() {
        show(state(Member("karan", "Karan Mehta", "uid-karan")))
        composeRule.onNodeWithText("No UPI ID yet").assertIsDisplayed()
        composeRule.onNodeWithText("Pay ₹600 with UPI app").assertIsNotEnabled()
        composeRule.onNodeWithText(
            "Karan Mehta hasn't added a UPI ID yet. Ask Karan Mehta to add one in Profile, or record a cash payment.",
        ).assertIsDisplayed()
    }

    @Test
    fun `a guest without a UPI ID offers to add one`() {
        show(state(Member("arjun", "Arjun", null)))
        composeRule.onNodeWithText("Add Arjun's UPI ID").performClick()
        assertEquals(listOf("guest"), calls)
    }

    @Test
    fun `after a failed payment the record link changes`() {
        show(state(priya, PayOutcome.NOT_COMPLETED))
        composeRule.onNodeWithText(
            "The UPI app didn't report a successful payment, so nothing was recorded.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Payment didn't go through? Record payment").assertIsDisplayed()
    }
}
