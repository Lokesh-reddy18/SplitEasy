package com.split.easy.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.split.easy.data.preferences.UserPreferencesRepository
import com.split.easy.domain.model.CurrentUser
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.repository.GroupRepository
import com.split.easy.domain.repository.SyncStatusRepository
import com.split.easy.domain.usecase.GroupSummary
import com.split.easy.domain.usecase.HomeSummary
import com.split.easy.domain.usecase.ObserveHomeSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val user: CurrentUser,
    val isLoading: Boolean = true,
    val summary: HomeSummary = HomeSummary(emptyList()),
    val archivedCount: Int = 0,
    val pendingChanges: Int = 0,
    val lastUsedGroupId: String? = null,
) {
    /** Group picker order: the last-used group first, then the rest as on home (Q15). */
    val pickerGroups: List<GroupSummary>
        get() = summary.groups.sortedByDescending { it.group.id == lastUsedGroupId }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHomeSummary: ObserveHomeSummary,
    groupRepository: GroupRepository,
    syncStatusRepository: SyncStatusRepository,
    private val preferences: UserPreferencesRepository,
    currentUserProvider: CurrentUserProvider,
) : ViewModel() {

    private val user = currentUserProvider.currentUser()

    val uiState: StateFlow<HomeUiState> = combine(
        observeHomeSummary(),
        groupRepository.observeArchivedGroups().map { it.size },
        syncStatusRepository.observePendingChangeCount(),
        preferences.lastUsedGroupId,
    ) { summary, archivedCount, pendingChanges, lastUsed ->
        HomeUiState(
            user = user,
            isLoading = false,
            summary = summary,
            archivedCount = archivedCount,
            pendingChanges = pendingChanges,
            lastUsedGroupId = lastUsed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(user = user))

    /** Remembers the picked group so it's first next time. */
    fun onGroupPicked(groupId: String) {
        viewModelScope.launch { preferences.setLastUsedGroupId(groupId) }
    }
}
