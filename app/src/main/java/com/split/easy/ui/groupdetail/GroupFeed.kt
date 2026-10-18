package com.split.easy.ui.groupdetail

import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.GroupEvent
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.Settlement
import com.split.easy.domain.settlement.SettlementRules
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** How an expense affects you, shown on the right of its row. */
sealed interface ExpenseImpact {
    /** You paid and others owe you part of it: "you lent ₹4,800". */
    data class Lent(val paise: Long) : ExpenseImpact

    /** Someone else paid and you had a share: "you borrowed ₹480". */
    data class Borrowed(val paise: Long) : ExpenseImpact

    /** You neither paid nor had a share: "not involved". */
    data object NotInvolved : ExpenseImpact

    /** You paid only for yourself, so nothing is owed either way. */
    data object NoBalance : ExpenseImpact
}

/** A row in the group feed. Names are resolved here; the screen turns them into sentences. */
sealed interface FeedItem {
    val key: String

    data class ExpenseItem(
        val expense: Expense,
        /** Null when the payer is you. */
        val payerName: String?,
        val impact: ExpenseImpact,
    ) : FeedItem {
        override val key = "expense-${expense.id}"
    }

    data class SettlementItem(
        val settlement: Settlement,
        /** Null when it's you. */
        val fromName: String?,
        val toName: String?,
        /** You may undo a confirmed payment (Q6, M1-Q6). */
        val canUndo: Boolean,
    ) : FeedItem {
        override val key = "settlement-${settlement.id}"
    }

    data class EventItem(
        val event: GroupEvent,
        /** Null when it's you. */
        val actorName: String?,
        val targetName: String,
    ) : FeedItem {
        override val key = "event-${event.id}"
    }
}

data class FeedDay(val date: LocalDate, val items: List<FeedItem>)

/**
 * Merges expenses, settlements and group events into days, newest first. Within a day, the most
 * recent comes first (expense creation time, settlement update time, event time).
 */
fun buildFeed(
    members: List<Member>,
    myMemberId: String?,
    expenses: List<Expense>,
    settlements: List<Settlement>,
    events: List<GroupEvent>,
    isGroupArchived: Boolean,
    zone: ZoneId = ZoneId.systemDefault(),
): List<FeedDay> {
    val byId = members.associateBy { it.id }
    fun nameOf(memberId: String): String? = if (memberId ==
        myMemberId
    ) {
        null
    } else {
        byId[memberId]?.displayName ?: "?"
    }
    val me = myMemberId?.let { byId[it] }

    data class Dated(val date: LocalDate, val time: Long, val item: FeedItem)

    val expenseRows = expenses.filterNot { it.isDeleted }.map { expense ->
        Dated(
            expense.date,
            expense.createdAt,
            FeedItem.ExpenseItem(
                expense,
                nameOf(expense.paidByMemberId),
                impactOf(expense, myMemberId),
            ),
        )
    }
    val settlementRows = settlements.filterNot { it.isDeleted }.map { settlement ->
        val payer = byId[settlement.fromMemberId]
        val payee = byId[settlement.toMemberId]
        val canUndo = me != null && payer != null && payee != null && !isGroupArchived &&
            SettlementRules.canDelete(
                user = me,
                settlement = settlement,
                payer = payer,
                payee = payee,
            )
        Dated(
            settlement.date,
            settlement.updatedAt,
            FeedItem.SettlementItem(
                settlement,
                nameOf(settlement.fromMemberId),
                nameOf(settlement.toMemberId),
                canUndo,
            ),
        )
    }
    val eventRows = events.map { event ->
        val date = Instant.ofEpochMilli(event.createdAt).atZone(zone).toLocalDate()
        Dated(
            date,
            event.createdAt,
            FeedItem.EventItem(
                event,
                nameOf(event.actorMemberId),
                byId[event.targetMemberId]?.displayName ?: "?",
            ),
        )
    }

    return (expenseRows + settlementRows + eventRows)
        .sortedWith(compareByDescending<Dated> { it.date }.thenByDescending { it.time })
        .groupBy { it.date }
        .map { (date, rows) -> FeedDay(date, rows.map { it.item }) }
}

fun impactOf(expense: Expense, myMemberId: String?): ExpenseImpact {
    val myShare = myMemberId?.let { expense.sharesPaise[it] } ?: 0L
    val iPaid = myMemberId != null && expense.paidByMemberId == myMemberId
    return when {
        iPaid && expense.amountPaise > myShare -> ExpenseImpact.Lent(expense.amountPaise - myShare)
        iPaid -> ExpenseImpact.NoBalance
        myShare > 0 -> ExpenseImpact.Borrowed(myShare)
        else -> ExpenseImpact.NotInvolved
    }
}
