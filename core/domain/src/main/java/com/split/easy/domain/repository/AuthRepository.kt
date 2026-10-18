package com.split.easy.domain.repository

import com.split.easy.domain.model.CurrentUser
import kotlinx.coroutines.flow.Flow

enum class SignInResult {
    SUCCESS,

    /** No connection to Google or Firebase. */
    NETWORK_ERROR,
    FAILED,
}

/** Google sign-in through Firebase Auth (FR-1). Firebase remembers the user, so it works offline. */
interface AuthRepository {
    /** The signed-in user right now, or null. */
    fun currentUser(): CurrentUser?

    /** Emits whenever someone signs in or out. */
    val signedInUser: Flow<CurrentUser?>

    /** Signs in to Firebase with the ID token from Google's account picker. */
    suspend fun signInWithGoogle(idToken: String): SignInResult

    suspend fun signOut()
}
