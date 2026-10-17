package com.split.easy.domain.balance

import java.util.PriorityQueue
import kotlin.math.min

/**
 * Turns net balances into a short list of payments that settles everyone (FR-7).
 *
 * Greedy method: repeatedly match the member who owes the most with the member who is owed
 * the most, and record a payment of the smaller of the two amounts. One of them is always
 * fully settled, so there are at most n − 1 payments. Two priority queues make it O(n log n).
 *
 * Ties are broken by member order (the order of the input map), so every device shows the
 * same list.
 */
object SettlementSimplifier {

    fun simplify(netBalances: Map<String, Long>): List<SuggestedPayment> {
        require(netBalances.values.sum() == 0L) { "Net balances must add up to zero" }

        val memberOrder = netBalances.keys.withIndex().associate { (index, id) -> id to index }
        val largestFirst = compareByDescending<Pair<String, Long>> { it.second }
            .thenBy { memberOrder.getValue(it.first) }

        // Each queue holds (memberId, amount still to settle), largest amount first.
        val creditors = PriorityQueue(largestFirst)
        val debtors = PriorityQueue(largestFirst)
        netBalances.forEach { (memberId, net) ->
            when {
                net > 0 -> creditors.add(memberId to net)
                net < 0 -> debtors.add(memberId to -net)
            }
        }

        val payments = mutableListOf<SuggestedPayment>()
        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val (creditor, owed) = creditors.poll()!!
            val (debtor, owes) = debtors.poll()!!
            val amount = min(owed, owes)
            payments += SuggestedPayment(debtor, creditor, amount)

            if (owed > amount) creditors.add(creditor to owed - amount)
            if (owes > amount) debtors.add(debtor to owes - amount)
        }
        return payments
    }
}
