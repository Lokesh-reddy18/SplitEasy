package com.split.easy.domain.expense

import com.split.easy.domain.model.Member
import com.split.easy.domain.money.MAX_AMOUNT_PAISE
import java.time.LocalDate

/** Expense rules: Q11 (who can edit) and section E (amount cap, date window). */
object ExpenseRules {

    /** People plan trips, so dates up to 30 days ahead are allowed. Past dates have no limit. */
    const val MAX_DAYS_AHEAD = 30L

    /** Descriptions are required and short, since they're the row title in the feed (M2-Q3). */
    const val MAX_DESCRIPTION_LENGTH = 60

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
