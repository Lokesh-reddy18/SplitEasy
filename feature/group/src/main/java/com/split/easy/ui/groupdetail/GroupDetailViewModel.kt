package com.split.easy.ui.groupdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.split.easy.data.preferences.UserPreferencesRepository
import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.Group
import com.split.easy.domain.model.GroupEvent
import com.split.easy.domain.model.Settlement
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
    /** "Add your UPI ID so friends can pay you" (18, M2-Q7). */
    val showUpiPrompt: Boolean = false,
) {
    /** You can add expenses and settle up only in a group you're in that isn't archived. */
    val canChangeThings: Boolean
        get() = isMember && !isArchived
}

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    observeGroupBalances: ObserveGroupBalances,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    /** Navigation stores the GroupDetailRoute's property under its name. */
    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private data class Inputs(
        val group: Group?,
        val balances: GroupBalances,
        val expenses: List<Expense>,
        val settlements: List<Settlement>,
        val events: List<GroupEvent>,
    )

    val uiState: StateFlow<GroupDetailUiState> = combine(
        groupRepository.observeGroup(groupId),
        observeGroupBalances(groupId),
        expenseRepository.observeExpenses(groupId),
        settlementRepository.observeSettlements(groupId),
        memberRepository.observeEvents(groupId),
    ) { group, balances, expenses, settlements, events ->
        Inputs(group, balances, expenses, settlements, events)
    }
        .combine(preferences.dismissedUpiPromptGroups) { inputs, dismissed ->
            toState(
                inputs,
                groupId in dismissed,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupDetailUiState())

    private fun toState(inputs: Inputs, promptDismissed: Boolean): GroupDetailUiState {
        val (group, balances, expenses, settlements, events) = inputs
        val isArchived = group?.isArchived ?: false
        val me = balances.members.firstOrNull { it.id == balances.myMemberId }
        return GroupDetailUiState(
            isLoading = false,
            groupName = group?.name.orEmpty(),
            memberNames = balances.members
                .filterNot { it.isRemoved }
                .map { if (it.id == balances.myMemberId) null else it.displayName },
            isArchived = isArchived,
            isMember = me != null,
            myNetPaise = balances.myNetPaise,
            settleUpTarget = settleUpTarget(balances),
            feed = buildFeed(balances.members, me?.id, expenses, settlements, events, isArchived),
            showUpiPrompt = me != null && me.upiId == null && balances.myNetPaise > 0 &&
                !isArchived && !promptDismissed,
        )
    }

    /** Undo a confirmed payment after the confirmation dialog (Q6). */
    fun onUndoSettlement(settlementId: String) {
        viewModelScope.launch { settlementRepository.deleteSettlement(settlementId) }
    }

    /** "Not now": hidden for this group; Profile keeps the option. */
    fun onDismissUpiPrompt() {
        viewModelScope.launch { preferences.dismissUpiPrompt(groupId) }
    }

    fun onSaveMyUpiId(upiId: String) {
        viewModelScope.launch { memberRepository.setMyUpiId(upiId) }
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
