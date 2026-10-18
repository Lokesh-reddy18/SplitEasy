package com.split.easy.domain.usecase

import com.split.easy.domain.balance.BalanceCalculator
import com.split.easy.domain.balance.PayableAmounts
import com.split.easy.domain.balance.SettlementSimplifier
import com.split.easy.domain.balance.SuggestedPayment
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.Settlement
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.repository.ExpenseRepository
import com.split.easy.domain.repository.MemberRepository
import com.split.easy.domain.repository.SettlementRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Everything the Balances and Group detail screens need about who owes whom. */
data class GroupBalances(
    /** Everyone who was ever in the group, in member order. */
    val members: List<Member>,
    val netBalances: Map<String, Long>,
    val suggestions: List<SuggestedPayment>,
    /** Payments still waiting for the payee to confirm. They don't change balances (Q4). */
    val pendingSettlements: List<Settlement>,
    /** The current user's member id in this group, or null if they're not in it. */
    val myMemberId: String?,
) {
    val myNetPaise: Long
        get() = myMemberId?.let { netBalances[it] } ?: 0L

    /** What [fromMemberId] can pay [toMemberId] now: the suggestion minus pending payments (Q4, Q5). */
    fun payable(fromMemberId: String, toMemberId: String): Long =
        PayableAmounts.payable(fromMemberId, toMemberId, suggestions, pendingSettlements)
}

/** Recomputes a group's balances whenever its members, expenses or settlements change. */
class ObserveGroupBalances @Inject constructor(
    private val memberRepository: MemberRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val currentUserProvider: CurrentUserProvider,
) {
    operator fun invoke(groupId: String): Flow<GroupBalances> = combine(
        memberRepository.observeMembers(groupId),
        expenseRepository.observeExpenses(groupId),
        settlementRepository.observeSettlements(groupId),
    ) { members, expenses, settlements ->
        val net = BalanceCalculator.netBalances(members.map { it.id }, expenses, settlements)
        val userId = currentUserProvider.currentUser().userId
        GroupBalances(
            members = members,
            netBalances = net,
            suggestions = SettlementSimplifier.simplify(net),
            pendingSettlements = settlements.filter { it.isOpenPending },
            myMemberId = members.firstOrNull { it.userId == userId && !it.isRemoved }?.id,
        )
    }
}
