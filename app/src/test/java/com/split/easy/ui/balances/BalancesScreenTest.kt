package com.split.easy.ui.balances

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.split.easy.domain.balance.BalanceCalculator
import com.split.easy.domain.balance.CoorgTrip
import com.split.easy.domain.balance.SettlementSimplifier
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.usecase.GroupBalances
import com.split.easy.ui.theme.SplitEasyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w390dp-h844dp")
class BalancesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun state(
        me: String,
        method: SettlementMethod = SettlementMethod.UPI,
    ): BalancesUiState {
        val members = CoorgTrip.members.mapIndexed { i, id ->
            Member(
                id,
                if (id ==
                    "you"
                ) {
                    "Ananya"
                } else {
                    id.replaceFirstChar { it.uppercase() }
                },
                "uid-$id",
                createdAt = i.toLong(),
            )
        }
        val net = BalanceCalculator.netBalances(
            CoorgTrip.members,
            CoorgTrip.expenses,
            CoorgTrip.settlements,
        )
        val pending = CoorgTrip.settlements.filter { it.isOpenPending }
            .map {
                it.copy(
                    method = method,
                    upiTxnId = if (method ==
                        SettlementMethod.UPI
                    ) {
                        "481022756631"
                    } else {
                        null
                    },
                )
            }
        return buildBalances(
            "Coorg trip",
            false,
            GroupBalances(members, net, SettlementSimplifier.simplify(net), pending, me),
        )
    }

    private val calls = mutableListOf<String>()

    private fun show(state: BalancesUiState) = composeRule.setContent {
        SplitEasyTheme {
            BalancesScreen(
                state = state,
                onBack = {},
                onConfirm = { calls += "confirm $it" },
                onReject = { calls += "reject $it" },
                onCancel = { calls += "cancel $it" },
                onPay = { calls += "pay $it" },
            )
        }
    }

    @Test
    fun `payee confirms a UPI payment, as in design 05`() {
        show(state("you"))
        composeRule.onNodeWithText("NEEDS YOUR CONFIRMATION").assertIsDisplayed()
        composeRule.onNodeWithText("Ravi paid you ₹1,200").assertIsDisplayed()
        composeRule.onNodeWithText("via UPI · Ref 4810 2275 6631").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Ravi's UPI app reported this payment as successful. Check that it reached your account, then confirm.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("4 payments settle everyone").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Arjun to Meera, ₹1,800").assertExists()

        composeRule.onNodeWithText("Confirm").performClick()
        composeRule.onNodeWithText("Not received").performClick()
        assertEquals(2, calls.size)
        assertEquals(listOf("confirm", "reject"), calls.map { it.substringBefore(' ') })
    }

    @Test
    fun `cash payments use the cash wording`() {
        show(state("you", SettlementMethod.CASH))
        composeRule.onNodeWithText(
            "Ravi says they paid you ₹1,200 in cash. Confirm once you've received it.",
        ).assertIsDisplayed()
    }

    @Test
    fun `payer sees Waiting with Cancel and Pay on remaining debts, as in design 22`() {
        show(state("ravi"))
        composeRule.onNodeWithText("WAITING FOR CONFIRMATION").assertIsDisplayed()
        composeRule.onNodeWithText("You paid Ananya ₹1,200").assertIsDisplayed()
        composeRule.onNodeWithText("via UPI · waiting for Ananya to confirm").assertIsDisplayed()
        composeRule.onNodeWithText("Waiting").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(listOf("cancel"), calls.map { it.substringBefore(' ') })
    }
}
