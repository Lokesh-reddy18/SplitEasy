package com.split.easy.ui.expense

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.split.easy.domain.model.Category
import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.SplitType
import com.split.easy.ui.expense.detail.ExpenseDetailScreen
import com.split.easy.ui.expense.detail.ExpenseDetailUiState
import com.split.easy.ui.expense.editor.EditorActions
import com.split.easy.ui.expense.editor.ExpenseEditorScreen
import com.split.easy.ui.expense.editor.ExpenseEditorUiState
import com.split.easy.ui.expense.editor.evaluate
import com.split.easy.ui.expense.editor.newExpenseForm
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
class ExpenseScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 10)
    private val members = listOf("You", "Priya", "Meera", "Ravi", "Arjun").map {
        Member(it.lowercase(), it, "uid-${it.lowercase()}")
    }

    @Test
    fun `editor shows equal shares and the assigned footer like the design`() {
        val form = newExpenseForm("you", members.map { it.id }, today)
            .copy(
                amountText = "2400",
                description = "Dinner at Raintree",
                paidByMemberId = "priya",
                category = Category.FOOD,
            )
        val state = ExpenseEditorUiState(
            isLoading = false,
            groupName = "Coorg trip",
            members = members,
            myMemberId = "you",
            form = form,
            result = form.evaluate(members.map { it.id }, today),
            today = today,
        )
        composeRule.setContent { SplitEasyTheme { ExpenseEditorScreen(state, EditorActions()) } }

        composeRule.onNodeWithText("Add expense").assertIsDisplayed()
        // Priya is on the "Paid by" tile and in the list of people.
        assertEquals(2, composeRule.onAllNodesWithText("Priya").fetchSemanticsNodes().size)
        composeRule.onNodeWithText("10 Oct").assertIsDisplayed()
        assertEquals(5, composeRule.onAllNodesWithText("₹480").fetchSemanticsNodes().size)
        composeRule.onNodeWithText("₹2,400 of ₹2,400 assigned").assertIsDisplayed()
        composeRule.onNodeWithText("5 people").assertIsDisplayed()
    }

    @Test
    fun `closing with changes asks before discarding`() {
        val form = newExpenseForm("you", members.map { it.id }, today).copy(amountText = "50")
        val state = ExpenseEditorUiState(
            isLoading = false,
            members = members,
            myMemberId = "you",
            form = form,
            result = form.evaluate(members.map { it.id }, today),
            hasChanges = true,
            today = today,
        )
        var closed = 0
        composeRule.setContent {
            SplitEasyTheme { ExpenseEditorScreen(state, EditorActions(onClose = { closed++ })) }
        }
        composeRule.onNodeWithText("Close", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNode(androidx.compose.ui.test.hasContentDescription("Close")).performClick()
        composeRule.onNodeWithText("Discard this expense?").assertIsDisplayed()
        composeRule.onNodeWithText("Discard").performClick()
        assertEquals(1, closed)
    }

    private val dinner = Expense(
        id = "dinner",
        groupId = "g1",
        paidByMemberId = "priya",
        amountPaise = 2_40_000,
        description = "Dinner at Raintree",
        category = Category.FOOD,
        date = today,
        splitType = SplitType.EQUAL,
        sharesPaise = members.associate { it.id to 48_000L },
        createdByUserId = "uid-you",
        createdAt = 0,
    )

    @Test
    fun `detail shows shares, and its creator can delete after confirming`() {
        var deleted = 0
        val state =
            ExpenseDetailUiState(
                isLoading = false,
                expense = dinner,
                groupName = "Coorg trip",
                members = members,
                myMemberId = "you",
                canEdit = true,
            )
        composeRule.setContent {
            SplitEasyTheme {
                ExpenseDetailScreen(state, onBack = {}, onEdit = {}, onDelete = { deleted++ })
            }
        }

        composeRule.onNodeWithText("Priya paid · split equally between 5").assertIsDisplayed()
        composeRule.onNodeWithText("paid").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("Delete this expense?").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Dinner at Raintree · ₹2,400. Balances in Coorg trip update for everyone.",
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText("Delete")[1].performClick()
        assertEquals(1, deleted)
    }

    @Test
    fun `someone else's expense is read-only`() {
        val state = ExpenseDetailUiState(
            isLoading = false,
            expense = dinner,
            members = members,
            myMemberId = "you",
            canEdit = false,
            creatorName = "Priya",
        )
        composeRule.setContent {
            SplitEasyTheme { ExpenseDetailScreen(state, onBack = {}, onEdit = {}, onDelete = {}) }
        }
        composeRule.onNodeWithText("Only Priya can edit this expense").assertIsDisplayed()
        composeRule.onNodeWithText("Edit").assertDoesNotExist()
    }
}
