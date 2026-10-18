package com.split.easy.ui.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.split.easy.domain.group.GroupRules
import com.split.easy.domain.model.Group
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.repository.GroupRepository
import com.split.easy.domain.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArchivedGroupRow(
    val group: Group,
    val canUnarchive: Boolean,
    /** Shown as "Meera can unarchive" when you can't. */
    val creatorName: String?,
)

data class ArchivedGroupsUiState(
    val isLoading: Boolean = true,
    val rows: List<ArchivedGroupRow> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArchivedGroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    memberRepository: MemberRepository,
    currentUserProvider: CurrentUserProvider,
) : ViewModel() {

    private val userId = currentUserProvider.currentUser().userId

    val uiState: StateFlow<ArchivedGroupsUiState> = groupRepository.observeArchivedGroups()
        .flatMapLatest { groups ->
            if (groups.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    groups.map { group ->
                        memberRepository.observeMembers(group.id).map { members ->
                            ArchivedGroupRow(
                                group = group,
                                canUnarchive = GroupRules.canUnarchive(userId, group),
                                creatorName = members.firstOrNull {
                                    it.userId ==
                                        group.createdByUserId
                                }?.displayName,
                            )
                        }
                    },
                ) { it.toList() }
            }
        }
        .map { ArchivedGroupsUiState(isLoading = false, rows = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArchivedGroupsUiState())

    fun onUnarchive(groupId: String) {
        viewModelScope.launch { groupRepository.setArchived(groupId, isArchived = false) }
    }
}
