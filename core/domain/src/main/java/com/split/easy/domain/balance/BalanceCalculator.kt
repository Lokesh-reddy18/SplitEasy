package com.split.easy.domain.balance

import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.Settlement

/**
 * Works out each member's net balance in a group (PRD "Settlement algorithm"):
 *
 *     net(i) = paid by i − shares of i + settlements sent by i − settlements received by i
 *
 * Positive means the member is owed money; negative means they owe. Deleted expenses and
 * pending, rejected or deleted settlements are ignored. The balances always add up to zero.
 */
object BalanceCalculator {

    /**
     * [memberIds] sets the order of the result (member order) and should include removed members.
     * Anyone who appears in an expense or settlement but not in [memberIds] is added at the end.
     */
    fun netBalances(
        memberIds: List<String>,
        expenses: List<Expense>,
        settlements: List<Settlement>,
    ): Map<String, Long> {
        val net = LinkedHashMap<String, Long>()
        memberIds.forEach { net[it] = 0L }

        fun add(memberId: String, paise: Long) {
            net[memberId] = (net[memberId] ?: 0L) + paise
        }

        expenses.filterNot { it.isDeleted }.forEach { expense ->
            add(expense.paidByMemberId, expense.amountPaise)
            expense.sharesPaise.forEach { (memberId, share) -> add(memberId, -share) }
        }
        settlements.filter { it.countsTowardBalances }.forEach { settlement ->
            add(settlement.fromMemberId, settlement.amountPaise)
            add(settlement.toMemberId, -settlement.amountPaise)
        }
        return net
    }
}
