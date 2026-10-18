package com.split.easy.ui.expense

import com.split.easy.domain.model.Category
import com.split.easy.domain.model.SplitType
import com.split.easy.ui.expense.editor.AmountError
import com.split.easy.ui.expense.editor.ExpenseForm
import com.split.easy.ui.expense.editor.SplitStatus
import com.split.easy.ui.expense.editor.evaluate
import com.split.easy.ui.expense.editor.newExpenseForm
import com.split.easy.ui.expense.editor.toDraft
import com.split.easy.ui.expense.editor.toForm
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseFormTest {

    private val today = LocalDate.of(2026, 10, 10)
    private val coorg = listOf("you", "priya", "meera", "ravi", "arjun")
    private val blank = newExpenseForm("you", coorg, today)

    private fun ExpenseForm.check() = evaluate(coorg, today)

    @Test
    fun `a new expense is paid by you, Other, today, split equally between everyone`() {
        assertEquals("you", blank.paidByMemberId)
        assertEquals(Category.OTHER, blank.category)
        assertEquals(today, blank.date)
        assertEquals(SplitType.EQUAL, blank.splitType)
        assertEquals(coorg.toSet(), blank.includedMemberIds)
    }

    @Test
    fun `design example - 2,400 paid by Priya, equally between five`() {
        val form = blank.copy(
            amountText = "2400",
            description = "Dinner at Raintree",
            paidByMemberId = "priya",
        )
        val result = form.check()
        assertTrue(result.isValid)
        assertEquals(coorg.associateWith { 48_000L }, result.sharesPaise)
        assertEquals(SplitStatus.Balanced(2_40_000, 5), result.splitStatus)
    }

    @Test
    fun `amount problems are reported, and the split waits for an amount`() {
        assertEquals(AmountError.MISSING, blank.check().amountError)
        assertEquals(SplitStatus.NeedsAmount, blank.check().splitStatus)
        assertEquals(
            AmountError.TOO_MANY_DECIMALS,
            blank.copy(amountText = "10.555").check().amountError,
        )
        assertEquals(AmountError.INVALID, blank.copy(amountText = "1.2.3").check().amountError)
        assertEquals(AmountError.NOT_POSITIVE, blank.copy(amountText = "0").check().amountError)
        assertEquals(
            AmountError.ABOVE_LIMIT,
            blank.copy(amountText = "1000000.01").check().amountError,
        )
    }

    @Test
    fun `description is required and dates can be at most 30 days ahead`() {
        val form = blank.copy(amountText = "100")
        assertTrue(form.check().isDescriptionMissing)
        assertFalse(form.check().isValid)
        assertTrue(
            form.copy(description = "x", date = today.plusDays(31)).check().isDateTooFarAhead,
        )
        assertTrue(form.copy(description = "x", date = today.plusDays(30)).check().isValid)
    }

    @Test
    fun `nobody ticked can't be split`() {
        val form = blank.copy(amountText = "100", description = "x", includedMemberIds = emptySet())
        assertEquals(SplitStatus.NoParticipants, form.check().splitStatus)
    }

    @Test
    fun `exact amounts report what's left, what's over, and bad entries`() {
        val exact = blank.copy(amountText = "1000", description = "x", splitType = SplitType.EXACT)
        assertEquals(SplitStatus.ExactLeft(1_00_000), exact.check().splitStatus)
        assertEquals(
            SplitStatus.ExactLeft(40_000),
            exact.copy(exactTexts = mapOf("you" to "600")).check().splitStatus,
        )
        assertEquals(
            SplitStatus.ExactOver(50_000),
            exact.copy(exactTexts = mapOf("you" to "700", "priya" to "800")).check().splitStatus,
        )
        assertEquals(
            SplitStatus.ExactOver(20_000),
            exact.copy(exactTexts = mapOf("you" to "1200")).check().splitStatus,
        )
        assertEquals(
            SplitStatus.InvalidEntry,
            exact.copy(exactTexts = mapOf("you" to "1.234")).check().splitStatus,
        )

        val balanced = exact.copy(
            exactTexts = mapOf(
                "you" to "600",
                "priya" to "400",
                "meera" to "",
                "ravi" to "0",
            ),
        )
        assertEquals(mapOf("you" to 60_000L, "priya" to 40_000L), balanced.check().sharesPaise)
        assertTrue(balanced.check().isValid)
    }

    @Test
    fun `percentages must reach 100 and keep what was typed`() {
        val percent = blank.copy(
            amountText = "100",
            description = "x",
            splitType = SplitType.PERCENT,
        )
        assertEquals(
            SplitStatus.PercentLeft(1),
            percent.copy(
                percentTexts = mapOf(
                    "you" to "33.33",
                    "priya" to "33.33",
                    "meera" to "33.33",
                ),
            ).check().splitStatus,
        )
        assertEquals(
            SplitStatus.PercentOver(1_000),
            percent.copy(
                percentTexts = mapOf(
                    "you" to "60",
                    "priya" to "50",
                ),
            ).check().splitStatus,
        )
        assertEquals(
            SplitStatus.InvalidEntry,
            percent.copy(percentTexts = mapOf("you" to "120")).check().splitStatus,
        )

        val balanced = percent.copy(
            percentTexts = mapOf(
                "you" to "33.33",
                "priya" to "33.33",
                "meera" to "33.34",
            ),
        )
        val result = balanced.check()
        assertEquals(
            mapOf("you" to 3_333L, "priya" to 3_333L, "meera" to 3_334L),
            result.sharesPaise,
        )
        assertEquals(
            mapOf("you" to 3_333, "priya" to 3_333, "meera" to 3_334),
            result.percentBasisPoints,
        )
    }

    @Test
    fun `saving and reopening an expense keeps what was entered`() {
        val form = blank.copy(
            amountText = "100",
            description = " Snacks ",
            category = Category.FOOD,
            splitType = SplitType.PERCENT,
            percentTexts = mapOf("you" to "33.33", "priya" to "66.67"),
        )
        val draft = form.toDraft("g1", form.check())
        assertEquals("Snacks", draft.description)
        assertEquals(mapOf("you" to 3_333, "priya" to 6_667), draft.percentBasisPoints)

        val saved = com.split.easy.domain.model.Expense(
            id = "e1",
            groupId = "g1",
            paidByMemberId = draft.paidByMemberId,
            amountPaise = draft.amountPaise,
            description = draft.description,
            category = draft.category,
            date = draft.date,
            splitType = draft.splitType,
            sharesPaise = draft.sharesPaise,
            percentBasisPoints = draft.percentBasisPoints,
            createdByUserId = "uid-you",
            createdAt = 0,
        )
        val reopened = saved.toForm()
        assertEquals("100", reopened.amountText)
        assertEquals(mapOf("you" to "33.33", "priya" to "66.67"), reopened.percentTexts)
        assertEquals(draft.sharesPaise, reopened.check().sharesPaise)
    }

    @Test
    fun `exact amounts and equal participants come back when editing`() {
        val exact = com.split.easy.domain.model.Expense(
            id = "e1",
            groupId = "g1",
            paidByMemberId = "you",
            amountPaise = 2_40_050,
            description = "Cab",
            category = Category.TRAVEL,
            date = today,
            splitType = SplitType.EXACT,
            sharesPaise = mapOf("you" to 2_00_050, "ravi" to 40_000),
            createdByUserId = "uid-you",
            createdAt = 0,
        )
        val form = exact.toForm()
        assertEquals("2400.50", form.amountText)
        assertEquals(mapOf("you" to "2000.50", "ravi" to "400"), form.exactTexts)
        assertEquals(setOf("you", "ravi"), form.includedMemberIds)
    }
}
