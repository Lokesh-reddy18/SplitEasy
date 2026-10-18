package com.split.easy.ui.groupsettings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.split.easy.domain.group.GroupCheck
import com.split.easy.domain.group.GroupRules
import com.split.easy.domain.model.Group
import com.split.easy.domain.model.Member
import com.split.easy.domain.repository.GroupRepository
import com.split.easy.domain.repository.MemberRepository
import com.split.easy.domain.repository.SettlementRepository
import com.split.easy.domain.usecase.ObserveGroupBalances
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A member row with what the creator may do about them. */
data class MemberRow(
    val member: Member,
    val isMe: Boolean,
    val isCreator: Boolean,
    /** Remove (creator only): Allowed, SettleFirst(₹X), ResolvePendingFirst, or NotAllowed. */
    val removeCheck: GroupCheck,
)

data class GroupSettingsUiState(
    val isLoading: Boolean = true,
    val group: Group? = null,
    val me: Member? = null,
    val members: List<MemberRow> = emptyList(),
    val isCreator: Boolean = false,
    /** Rename and add guests (F5). */
    val memberActionCheck: GroupCheck = GroupCheck.NotAllowed,
    val archiveCheck: GroupCheck = GroupCheck.NotAllowed,
    val unarchiveCheck: GroupCheck = GroupCheck.NotAllowed,
    val leaveCheck: GroupCheck = GroupCheck.NotAllowed,
) {
    val canEditGuests: Boolean
        get() = memberActionCheck == GroupCheck.Allowed
}

/** Group settings (12): every action goes through GroupRules (F5, Q13, M1-Q7). */
@HiltViewModel
class GroupSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    settlementRepository: SettlementRepository,
    observeGroupBalances: ObserveGroupBalances,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val left = Channel<Unit>(Channel.BUFFERED)

    /** Emits after you leave the group, so the app can go back to Groups home. */
    val leftEvents = left.receiveAsFlow()

    val uiState: StateFlow<GroupSettingsUiState> = combine(
        groupRepository.observeGroup(groupId),
        observeGroupBalances(groupId),
        settlementRepository.observeSettlements(groupId),
    ) { group, balances, settlements ->
        val me = balances.members.firstOrNull { it.id == balances.myMemberId }
        if (group == null ||
            me == null
        ) {
            return@combine GroupSettingsUiState(isLoading = false, group = group)
        }
        val net = balances.netBalances
        GroupSettingsUiState(
            isLoading = false,
            group = group,
            me = me,
            isCreator = GroupRules.isCreator(me, group),
            members = balances.members.filterNot { it.isRemoved }.map { member ->
                MemberRow(
                    member = member,
                    isMe = member.id == me.id,
                    isCreator = GroupRules.isCreator(member, group),
                    removeCheck = GroupRules.checkRemoveMember(
                        me,
                        group,
                        member,
                        net[member.id] ?: 0L,
                        settlements,
                    ),
                )
            },
            memberActionCheck = GroupRules.checkMemberAction(me, group),
            archiveCheck = GroupRules.checkArchive(me, group, net, settlements),
            unarchiveCheck = GroupRules.checkUnarchive(me, group),
            leaveCheck = GroupRules.checkLeave(me, group, net[me.id] ?: 0L, settlements),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupSettingsUiState())

    private val state get() = uiState.value

    fun onRename(name: String) {
        if (state.memberActionCheck != GroupCheck.Allowed || name.isBlank()) return
        viewModelScope.launch { groupRepository.renameGroup(groupId, name.take(MAX_GROUP_NAME)) }
    }

    fun onAddGuest(name: String) {
        if (state.memberActionCheck != GroupCheck.Allowed || name.isBlank()) return
        viewModelScope.launch { memberRepository.addGuest(groupId, name.take(MAX_PERSON_NAME)) }
    }

    /** Any member may set a guest's UPI ID; the change is logged in the feed (F1). */
    fun onSetGuestUpiId(memberId: String, upiId: String) {
        val me = state.me ?: return
        if (!state.canEditGuests) return
        viewModelScope.launch { memberRepository.setGuestUpiId(memberId, me.id, upiId) }
    }

    fun onRemove(memberId: String) {
        val row = state.members.firstOrNull { it.member.id == memberId } ?: return
        if (row.removeCheck != GroupCheck.Allowed) return
        viewModelScope.launch { memberRepository.removeMember(memberId) }
    }

    fun onArchive() {
        if (state.archiveCheck != GroupCheck.Allowed) return
        viewModelScope.launch { groupRepository.setArchived(groupId, isArchived = true) }
    }

    fun onUnarchive() {
        if (state.unarchiveCheck != GroupCheck.Allowed) return
        viewModelScope.launch { groupRepository.setArchived(groupId, isArchived = false) }
    }

    fun onLeave() {
        val me = state.me ?: return
        if (state.leaveCheck != GroupCheck.Allowed) return
        viewModelScope.launch {
            memberRepository.removeMember(me.id)
            left.send(Unit)
        }
    }

    private companion object {
        const val MAX_GROUP_NAME = 40
        const val MAX_PERSON_NAME = 30
    }
}
