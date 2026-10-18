package com.split.easy.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.split.easy.data.demo.DemoDataLoader
import com.split.easy.data.preferences.UserPreferencesRepository
import com.split.easy.domain.model.CurrentUser
import com.split.easy.domain.model.ThemeSetting
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.repository.GroupRepository
import com.split.easy.domain.repository.MemberRepository
import com.split.easy.domain.repository.SyncStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: CurrentUser,
    /** Your UPI ID, the same on your member row in every group (F2). */
    val upiId: String? = null,
    val theme: ThemeSetting = ThemeSetting.SYSTEM,
    val pendingChanges: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    currentUserProvider: CurrentUserProvider,
    groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    syncStatusRepository: SyncStatusRepository,
    private val preferences: UserPreferencesRepository,
    private val demoDataLoader: DemoDataLoader,
) : ViewModel() {

    private val user = currentUserProvider.currentUser()

    /** Your UPI ID, read from any group you're in (they're kept in step). */
    private val myUpiId: Flow<String?> =
        combine(groupRepository.observeActiveGroups(), groupRepository.observeArchivedGroups()) {
                a,
                b,
            ->
            a +
                b
        }
            .flatMapLatest { groups ->
                if (groups.isEmpty()) {
                    flowOf(null)
                } else {
                    combine(groups.map { memberRepository.observeMembers(it.id) }) { lists ->
                        lists.flatMap { it.toList() }.firstOrNull {
                            it.userId == user.userId &&
                                it.upiId != null
                        }?.upiId
                    }
                }
            }

    val uiState: StateFlow<ProfileUiState> = combine(
        myUpiId,
        preferences.themeSetting,
        syncStatusRepository.observePendingChangeCount(),
    ) { upi, theme, pending -> ProfileUiState(user, upi, theme, pending) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState(user))

    private val demoLoaded = Channel<Unit>(Channel.BUFFERED)
    val demoLoadedEvents = demoLoaded.receiveAsFlow()

    fun onSaveUpiId(upiId: String) {
        viewModelScope.launch { memberRepository.setMyUpiId(upiId) }
    }

    fun onThemeChange(theme: ThemeSetting) {
        viewModelScope.launch { preferences.setThemeSetting(theme) }
    }

    fun onLoadDemoData() {
        viewModelScope.launch {
            demoDataLoader.load()
            demoLoaded.send(Unit)
        }
    }
}
