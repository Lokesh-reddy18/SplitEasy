package com.split.easy.ui.groupdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.split.easy.domain.balance.rupees
import com.split.easy.domain.model.Category
import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.model.SplitType
import com.split.easy.domain.testSettlement
import com.split.easy.ui.theme.SplitEasyTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w390dp-h844dp")
class GroupDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sat = LocalDate.of(2026, 10, 10)
    private val dinner = Expense(
        id = "dinner",
        groupId = "g1",
        paidByMemberId = "priya",
        amountPaise = rupees(2_400),
        description = "Dinner at Raintree",
        category = Category.FOOD,
        date = sat,
        splitType = SplitType.EQUAL,
        sharesPaise = mapOf("you" to rupees(480)),
        createdByUserId = "uid-you",
        createdAt = 1,
    )
    private val confirmed = testSettlement(
        "ravi",
        "you",
        rupees(1_200),
        SettlementStatus.CONFIRMED,
        id = "s1",
    ).copy(date = sat)

    private val state = GroupDetailUiState(
        isLoading = false,
        groupName = "Coorg trip",
        memberNames = listOf(null, "Priya", "Meera", "Ravi", "Arjun"),
        isMember = true,
        myNetPaise = rupees(1_200),
        feed = listOf(
            FeedDay(
                sat,
                listOf(
                    FeedItem.ExpenseItem(dinner, "Priya", ExpenseImpact.Borrowed(rupees(480))),
                    FeedItem.SettlementItem(
                        confirmed,
                        fromName = "Ravi",
                        toName = null,
                        canUndo = true,
                    ),
                ),
            ),
        ),
    )

    private var opened: String? = null
    private var undone: String? = null
    private var settleUps = 0

    private fun show(state: GroupDetailUiState) = composeRule.setContent {
        SplitEasyTheme {
            GroupDetailScreen(
                state = state,
                onBack = {},
                onOpenSettings = {},
                onSettleUp = { settleUps++ },
                onOpenBalances = {},
                onAddExpense = {},
                onOpenExpense = { opened = it },
                onUndoSettlement = { undone = it },
            )
        }
    }

    @Test
    fun `shows the balance, members and feed like the design`() {
        show(state)
        composeRule.onNodeWithText("You, Priya, Meera, Ravi, Arjun").assertIsDisplayed()
        composeRule.onNodeWithText("You are owed ₹1,200").assertIsDisplayed()
        composeRule.onNodeWithText("SAT, 10 OCT").assertIsDisplayed()
        composeRule.onNodeWithText("Priya paid ₹2,400").assertIsDisplayed()
        composeRule.onNodeWithText("you borrowed").assertIsDisplayed()
        composeRule.onNodeWithText("Ravi paid you").assertIsDisplayed()
        composeRule.onNodeWithText("Settle up").performClick()
        assertEquals(1, settleUps)
    }

    @Test
    fun `tapping an expense opens it, and undoing a payment asks first`() {
        show(state)
        composeRule.onNodeWithText("Dinner at Raintree").performClick()
        assertEquals("dinner", opened)

        composeRule.onNodeWithText("Ravi paid you").performClick()
        composeRule.onNodeWithText("Undo this payment?").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()
        assertEquals("s1", undone)
    }

    @Test
    fun `archived groups are read-only`() {
        show(state.copy(isArchived = true))
        composeRule.onNodeWithText(
            "This group is archived, so nothing can change.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Settle up").assertDoesNotExist()
        composeRule.onNodeWithText("Add expense", useUnmergedTree = true).assertDoesNotExist()
    }
}
