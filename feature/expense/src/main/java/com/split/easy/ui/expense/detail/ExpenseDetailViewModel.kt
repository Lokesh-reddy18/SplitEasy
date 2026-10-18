package com.split.easy.ui.expense.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.split.easy.domain.expense.ExpenseRules
import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.Member
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.repository.ExpenseRepository
import com.split.easy.domain.repository.GroupRepository
import com.split.easy.domain.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExpenseDetailUiState(
    val isLoading: Boolean = true,
    val expense: Expense? = null,
    val groupName: String = "",
    /** Everyone on the expense, in member order. */
    val members: List<Member> = emptyList(),
    val myMemberId: String? = null,
    /** Only the person who added it, and not in an archived group (Q11). */
    val canEdit: Boolean = false,
    /** Null when you added it. */
    val creatorName: String? = null,
) {
    fun nameOf(memberId: String): String? =
        if (memberId == myMemberId) null else members.firstOrNull { it.id == memberId }?.displayName
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository,
    memberRepository: MemberRepository,
    groupRepository: GroupRepository,
    currentUserProvider: CurrentUserProvider,
) : ViewModel() {

    val expenseId: String = checkNotNull(savedStateHandle["expenseId"])
    private val userId = currentUserProvider.currentUser().userId

    private val deleted = Channel<Unit>(Channel.BUFFERED)

    /** Emits after Delete, so the screen can close. */
    val deletedEvents = deleted.receiveAsFlow()

    val uiState: StateFlow<ExpenseDetailUiState> = expenseRepository.observeExpense(expenseId)
        .flatMapLatest { expense ->
            if (expense == null) {
                flowOf(ExpenseDetailUiState(isLoading = false))
            } else {
                combine(
                    memberRepository.observeMembers(expense.groupId),
                    groupRepository.observeGroup(expense.groupId),
                ) {
                        members,
                        group,
                    ->
                    val me = members.firstOrNull { it.userId == userId && !it.isRemoved }
                    val creator = members.firstOrNull { it.userId == expense.createdByUserId }
                    ExpenseDetailUiState(
                        isLoading = false,
                        expense = expense,
                        groupName = group?.name.orEmpty(),
                        members = members.filter {
                            it.id in expense.sharesPaise ||
                                it.id == expense.paidByMemberId
                        },
                        myMemberId = me?.id,
                        canEdit =
                            me != null &&
                                ExpenseRules.canEdit(
                                    me,
                                    expense.createdByUserId,
                                    group?.isArchived ?: false,
                                ),
                        creatorName = if (expense.createdByUserId ==
                            userId
                        ) {
                            null
                        } else {
                            creator?.displayName
                        },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseDetailUiState())

    fun onDelete() {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expenseId)
            deleted.send(Unit)
        }
    }
}
