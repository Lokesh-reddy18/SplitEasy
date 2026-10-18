package com.split.easy.ui.expense.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.split.easy.data.preferences.UserPreferencesRepository
import com.split.easy.domain.expense.ExpenseRules
import com.split.easy.domain.model.Category
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.SplitType
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.repository.ExpenseRepository
import com.split.easy.domain.repository.GroupRepository
import com.split.easy.domain.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpenseEditorUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val groupName: String = "",
    /** Who can pay or share, in member order. */
    val members: List<Member> = emptyList(),
    val myMemberId: String? = null,
    val form: ExpenseForm? = null,
    val result: FormResult? = null,
    /** Errors appear only after the first Save attempt, so a fresh form isn't covered in red. */
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
    val hasChanges: Boolean = false,
    val today: LocalDate = LocalDate.MIN,
) {
    val maxDate: LocalDate
        get() = today.plusDays(ExpenseRules.MAX_DAYS_AHEAD)
}

@HiltViewModel
class ExpenseEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    groupRepository: GroupRepository,
    memberRepository: MemberRepository,
    private val expenseRepository: ExpenseRepository,
    private val preferences: UserPreferencesRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val clock: Clock,
) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])
    private val expenseId: String? = savedStateHandle["expenseId"]

    private val _uiState = MutableStateFlow(ExpenseEditorUiState(isEditing = expenseId != null))
    val uiState: StateFlow<ExpenseEditorUiState> = _uiState.asStateFlow()

    private val saved = Channel<Unit>(Channel.BUFFERED)

    /** Emits once the expense is saved, so the screen can close. */
    val savedEvents = saved.receiveAsFlow()

    private var initialForm: ExpenseForm? = null

    init {
        viewModelScope.launch {
            val today = LocalDate.now(clock)
            val group = groupRepository.observeGroup(groupId).first()
            val allMembers = memberRepository.observeMembers(groupId).first()
            val userId = currentUserProvider.currentUser().userId
            val me = allMembers.firstOrNull { it.userId == userId && !it.isRemoved }
            val existing = expenseId?.let { expenseRepository.observeExpense(it).first() }

            // Current members, plus anyone who has since left but is on this expense (H4).
            val referenced = existing?.let { it.sharesPaise.keys + it.paidByMemberId }.orEmpty()
            val members = allMembers.filter { !it.isRemoved || it.id in referenced }
            val form = existing?.toForm()
                ?: newExpenseForm(
                    me?.id ?: members.first().id,
                    members.filterNot {
                        it.isRemoved
                    }.map { it.id },
                    today,
                )

            initialForm = form
            _uiState.value = ExpenseEditorUiState(
                isLoading = false,
                isEditing = existing != null,
                groupName = group?.name.orEmpty(),
                members = members,
                myMemberId = me?.id,
                form = form,
                result = form.evaluate(members.map { it.id }, today),
                today = today,
            )
        }
    }

    private fun updateForm(change: (ExpenseForm) -> ExpenseForm) {
        _uiState.update { state ->
            val form = state.form?.let(change) ?: return@update state
            state.copy(
                form = form,
                result = form.evaluate(state.members.map { it.id }, state.today),
                hasChanges = form != initialForm,
            )
        }
    }

    fun onAmountChange(text: String) = updateForm {
        it.copy(
            amountText = text.filter { c ->
                c.isDigit() ||
                    c == '.'
            },
        )
    }

    fun onDescriptionChange(text: String) =
        updateForm { it.copy(description = text.take(ExpenseRules.MAX_DESCRIPTION_LENGTH)) }

    fun onPaidByChange(memberId: String) = updateForm { it.copy(paidByMemberId = memberId) }

    fun onCategoryChange(category: Category) = updateForm { it.copy(category = category) }

    fun onDateChange(date: LocalDate) = updateForm { it.copy(date = date) }

    fun onSplitTypeChange(type: SplitType) = updateForm { it.copy(splitType = type) }

    fun onToggleIncluded(memberId: String) = updateForm { form ->
        val included = form.includedMemberIds
        form.copy(
            includedMemberIds = if (memberId in
                included
            ) {
                included - memberId
            } else {
                included + memberId
            },
        )
    }

    fun onExactChange(memberId: String, text: String) = updateForm {
        it.copy(
            exactTexts =
                it.exactTexts + (memberId to text.filter { c -> c.isDigit() || c == '.' }),
        )
    }

    fun onPercentChange(memberId: String, text: String) = updateForm {
        it.copy(
            percentTexts =
                it.percentTexts + (memberId to text.filter { c -> c.isDigit() || c == '.' }),
        )
    }

    fun onSave() {
        val state = _uiState.value
        val form = state.form ?: return
        val result = state.result ?: return
        if (state.isSaving) return
        if (!result.isValid) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val draft = form.toDraft(groupId, result)
            if (expenseId == null) {
                expenseRepository.addExpense(draft)
                preferences.setLastUsedGroupId(groupId) // first in the group picker next time (Q15)
            } else {
                expenseRepository.updateExpense(expenseId, draft)
            }
            saved.send(Unit)
        }
    }
}
