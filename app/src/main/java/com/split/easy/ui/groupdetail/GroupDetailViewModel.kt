package com.split.easy.ui.groupdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.split.easy.domain.repository.ExpenseRepository
import com.split.easy.domain.repository.GroupRepository
import com.split.easy.domain.repository.MemberRepository
import com.split.easy.domain.repository.SettlementRepository
import com.split.easy.domain.usecase.GroupBalances
import com.split.easy.domain.usecase.ObserveGroupBalances
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Where "Settle up" goes (Q14). */
sealed interface SettleUpTarget {
    /** You owe exactly one person: open the payment sheet for them. */
    data class Pay(val toMemberId: String) : SettleUpTarget

    /** You owe several people, or nobody: open Balances. */
    data object Balances : SettleUpTarget
}

data class GroupDetailUiState(
    val isLoading: Boolean = true,
    val groupName: String = "",
    /** Current members in member order; null stands for you. */
    val memberNames: List<String?> = emptyList(),
    val isArchived: Boolean = false,
    /** False once you've left the group (or it isn't yours). */
    val isMember: Boolean = false,
    val myNetPaise: Long = 0L,
    val settleUpTarget: SettleUpTarget = SettleUpTarget.Balances,
    val feed: List<FeedDay> = emptyList(),
) {
    /** You can add expenses and settle up only in a group you're in that isn't archived. */
    val canChangeThings: Boolean
        get() = isMember && !isArchived
}

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    groupRepository: GroupRepository,
    memberRepository: MemberRepository,
    expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    observeGroupBalances: ObserveGroupBalances,
) : ViewModel() {

    /** Navigation stores the GroupDetailRoute's property under its name. */
    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    val uiState: StateFlow<GroupDetailUiState> = combine(
        groupRepository.observeGroup(groupId),
        observeGroupBalances(groupId),
        expenseRepository.observeExpenses(groupId),
        settlementRepository.observeSettlements(groupId),
        memberRepository.observeEvents(groupId),
    ) { group, balances, expenses, settlements, events ->
        val isArchived = group?.isArchived ?: false
        GroupDetailUiState(
            isLoading = false,
            groupName = group?.name.orEmpty(),
            memberNames = balances.members
                .filterNot { it.isRemoved }
                .map { if (it.id == balances.myMemberId) null else it.displayName },
            isArchived = isArchived,
            isMember = balances.myMemberId != null,
            myNetPaise = balances.myNetPaise,
            settleUpTarget = settleUpTarget(balances),
            feed = buildFeed(
                balances.members,
                balances.myMemberId,
                expenses,
                settlements,
                events,
                isArchived,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupDetailUiState())

    /** Undo a confirmed payment after the confirmation dialog (Q6). */
    fun onUndoSettlement(settlementId: String) {
        viewModelScope.launch { settlementRepository.deleteSettlement(settlementId) }
    }
}

/** Q14: straight to paying if you owe exactly one person something payable, otherwise Balances. */
fun settleUpTarget(balances: GroupBalances): SettleUpTarget {
    val me = balances.myMemberId ?: return SettleUpTarget.Balances
    val payable = balances.suggestions.filter {
        it.fromMemberId == me &&
            balances.payable(me, it.toMemberId) > 0
    }
    return payable.singleOrNull()?.let { SettleUpTarget.Pay(it.toMemberId) }
        ?: SettleUpTarget.Balances
}
