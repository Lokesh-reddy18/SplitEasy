package com.split.easy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.split.easy.data.preferences.UserPreferencesRepository
import com.split.easy.data.user.SessionManager
import com.split.easy.domain.model.Session
import com.split.easy.domain.model.ThemeSetting
import com.split.easy.domain.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/** App-wide state the activity needs before any screen is shown: the theme and who is signed in. */
@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
    sessionManager: SessionManager,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {
    val themeSetting: StateFlow<ThemeSetting> = preferences.themeSetting
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeSetting.SYSTEM)

    /** Null until Firebase reports whether someone is signed in (a few milliseconds at start-up). */
    val session: StateFlow<Session?> = sessionManager.session
        // Upload anything left over from last time once someone is signed in (FR-10).
        .onEach { if (it is Session.SignedIn) syncScheduler.requestSync() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
