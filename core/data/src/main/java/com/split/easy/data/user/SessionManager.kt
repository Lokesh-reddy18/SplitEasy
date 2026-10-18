package com.split.easy.data.user

import com.split.easy.data.local.LocalDataEraser
import com.split.easy.data.preferences.UserPreferencesRepository
import com.split.easy.domain.model.Session
import com.split.easy.domain.repository.AuthRepository
import com.split.easy.domain.repository.SyncStatusRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformLatest

/**
 * Keeps the data on this phone tied to one account (M4-Q2, M4-Q3).
 *
 * The phone remembers whose data it holds. When a different account signs in (or the first one,
 * replacing M2's test data), everything except the theme is wiped before any screen shows it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SessionManager @Inject constructor(
    private val auth: AuthRepository,
    private val eraser: LocalDataEraser,
    private val preferences: UserPreferencesRepository,
    private val syncStatus: SyncStatusRepository,
) {
    val session: Flow<Session> = auth.signedInUser.transformLatest { user ->
        if (user == null) {
            emit(Session.SignedOut)
            return@transformLatest
        }
        if (preferences.dataOwnerId.first() != user.userId) {
            emit(Session.Preparing(user))
            clearLocalData()
            preferences.setDataOwnerId(user.userId)
        }
        emit(Session.SignedIn(user))
    }

    /** Changes saved on this phone that haven't reached Firestore yet. */
    suspend fun unsyncedChangeCount(): Int = syncStatus.observePendingChangeCount().first()

    /** Deletes this account's data from the phone, then signs out (M4-Q3). */
    suspend fun signOut() {
        clearLocalData()
        preferences.setDataOwnerId(null)
        auth.signOut()
    }

    private suspend fun clearLocalData() {
        eraser.eraseAll()
        preferences.clearAccountSettings()
    }
}
