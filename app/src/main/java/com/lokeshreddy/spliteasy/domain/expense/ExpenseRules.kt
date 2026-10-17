package com.lokeshreddy.spliteasy.domain.expense

import com.lokeshreddy.spliteasy.domain.model.Member
import com.lokeshreddy.spliteasy.domain.money.MAX_AMOUNT_PAISE
import java.time.LocalDate

/** Expense rules: Q11 (who can edit) and section E (amount cap, date window). */
object ExpenseRules {

    /** People plan trips, so dates up to 30 days ahead are allowed. Past dates have no limit. */
    const val MAX_DAYS_AHEAD = 30L

    /**
     * Only the person who added an expense can edit or delete it, and not in an archived group.
     * Everyone else sees it read-only, including after its creator has left the group.
     */
    fun canEdit(user: Member, expenseCreatedByUserId: String, isGroupArchived: Boolean): Boolean =
        !isGroupArchived && !user.isRemoved && user.userId == expenseCreatedByUserId

    fun check(amountPaise: Long, date: LocalDate, today: LocalDate): ExpenseCheck = when {
        amountPaise <= 0 -> ExpenseCheck.AMOUNT_NOT_POSITIVE
        amountPaise > MAX_AMOUNT_PAISE -> ExpenseCheck.AMOUNT_ABOVE_LIMIT
        date.isAfter(today.plusDays(MAX_DAYS_AHEAD)) -> ExpenseCheck.DATE_TOO_FAR_AHEAD
        else -> ExpenseCheck.OK
    }
}
