package com.split.easy.ui.balances

import com.split.easy.domain.model.Member
import com.split.easy.domain.model.Settlement
import com.split.easy.domain.settlement.SettlementRules
import com.split.easy.domain.usecase.GroupBalances

/** A pending payment with names resolved. A null name means you. */
data class PendingRow(
    val settlement: Settlement,
    val fromName: String?,
    val toName: String?,
    val canAct: Boolean,
)

/** "Ravi → You ₹1,200" with what you can do about it. */
data class SuggestionRow(
    val fromMemberId: String,
    val toMemberId: String,
    val fromName: String?,
    val toName: String?,
    val amountPaise: Long,
    /** Some of this is already paid and waiting for confirmation. */
    val hasPending: Boolean,
    /** What you can pay now (your debts only): the suggestion minus your pending payments (Q4). */
    val payablePaise: Long,
    val isMine: Boolean,
)

data class NetRow(val name: String?, val netPaise: Long)

data class BalancesUiState(
    val isLoading: Boolean = true,
    val groupName: String = "",
    val isReadOnly: Boolean = false,
    /** Payments to you, waiting for you (H2: "Needs your confirmation"). */
    val toConfirm: List<PendingRow> = emptyList(),
    /** Your payments, waiting for the payee (H2: "Waiting for confirmation"). */
    val waiting: List<PendingRow> = emptyList(),
    val suggestions: List<SuggestionRow> = emptyList(),
    val net: List<NetRow> = emptyList(),
)

/** Turns a group's balances into the four sections of the Balances screen. */
fun buildBalances(
    groupName: String,
    isArchived: Boolean,
    balances: GroupBalances,
): BalancesUiState {
    val me = balances.myMemberId
    val byId = balances.members.associateBy { it.id }
    fun name(id: String): String? = if (id == me) null else byId[id]?.displayName ?: "?"
    val myMember: Member? = me?.let { byId[it] }
    val readOnly = isArchived || myMember == null

    val pending = balances.pendingSettlements
    val toConfirm = pending.filter { it.toMemberId == me }.map {
        PendingRow(
            it,
            name(it.fromMemberId),
            null,
            !readOnly && SettlementRules.canConfirmOrReject(myMember!!, it),
        )
    }
    val waiting = pending.filter { it.fromMemberId == me }.map {
        PendingRow(
            it,
            null,
            name(it.toMemberId),
            !readOnly && SettlementRules.canCancel(myMember!!, it),
        )
    }
    val suggestions = balances.suggestions.map { s ->
        val mine = s.fromMemberId == me
        SuggestionRow(
            fromMemberId = s.fromMemberId,
            toMemberId = s.toMemberId,
            fromName = name(s.fromMemberId),
            toName = name(s.toMemberId),
            amountPaise = s.amountPaise,
            hasPending = pending.any {
                it.fromMemberId == s.fromMemberId &&
                    it.toMemberId == s.toMemberId
            },
            payablePaise = if (mine &&
                !readOnly
            ) {
                balances.payable(s.fromMemberId, s.toMemberId)
            } else {
                0L
            },
            isMine = mine,
        )
    }
    // Biggest creditor first, as in the design; removed members only while they still have a balance.
    val order = balances.members.withIndex().associate { (i, m) -> m.id to i }
    val net = balances.members
        .filter { !it.isRemoved || (balances.netBalances[it.id] ?: 0L) != 0L }
        .sortedWith(
            compareByDescending<Member> {
                balances.netBalances[it.id] ?: 0L
            }.thenBy { order[it.id] },
        )
        .map { NetRow(name(it.id), balances.netBalances[it.id] ?: 0L) }

    return BalancesUiState(false, groupName, readOnly, toConfirm, waiting, suggestions, net)
}

/** "481022756631" → "4810 2275 6631". */
fun formatUpiRef(ref: String): String = ref.chunked(4).joinToString(" ")
