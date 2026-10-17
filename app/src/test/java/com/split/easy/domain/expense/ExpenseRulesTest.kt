package com.split.easy.domain.expense

import com.split.easy.domain.model.Member
import com.split.easy.domain.money.MAX_AMOUNT_PAISE
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseRulesTest {

    private val today = LocalDate.of(2026, 10, 17)
    private val you = Member("m-you", userId = "uid-you")
    private val priya = Member("m-priya", userId = "uid-priya")

    @Test
    fun `only the creator can edit, and not in an archived group`() {
        assertTrue(
            ExpenseRules.canEdit(you, expenseCreatedByUserId = "uid-you", isGroupArchived = false),
        )
        assertFalse(
            ExpenseRules.canEdit(
                priya,
                expenseCreatedByUserId = "uid-you",
                isGroupArchived = false,
            ),
        )
        assertFalse(
            ExpenseRules.canEdit(you, expenseCreatedByUserId = "uid-you", isGroupArchived = true),
        )
        assertFalse(
            ExpenseRules.canEdit(you.copy(isRemoved = true), "uid-you", isGroupArchived = false),
        )
    }

    @Test
    fun `dates up to 30 days ahead are allowed`() {
        assertEquals(ExpenseCheck.OK, ExpenseRules.check(100, today.plusDays(30), today))
        assertEquals(
            ExpenseCheck.DATE_TOO_FAR_AHEAD,
            ExpenseRules.check(100, today.plusDays(31), today),
        )
        assertEquals(ExpenseCheck.OK, ExpenseRules.check(100, today.minusYears(3), today))
    }

    @Test
    fun `amount must be positive and within the cap`() {
        assertEquals(ExpenseCheck.AMOUNT_NOT_POSITIVE, ExpenseRules.check(0, today, today))
        assertEquals(ExpenseCheck.OK, ExpenseRules.check(MAX_AMOUNT_PAISE, today, today))
        assertEquals(
            ExpenseCheck.AMOUNT_ABOVE_LIMIT,
            ExpenseRules.check(MAX_AMOUNT_PAISE + 1, today, today),
        )
    }
}
