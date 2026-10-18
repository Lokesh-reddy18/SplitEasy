package com.split.easy.data.user

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.split.easy.domain.model.CurrentUser
import com.split.easy.domain.repository.AuthRepository
import com.split.easy.domain.repository.SignInResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    @param:ApplicationContext private val context: Context,
) : AuthRepository {

    override fun currentUser(): CurrentUser? = auth.currentUser?.toCurrentUser()

    override val signedInUser: Flow<CurrentUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toCurrentUser()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun signInWithGoogle(idToken: String): SignInResult = try {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        SignInResult.SUCCESS
    } catch (_: FirebaseNetworkException) {
        SignInResult.NETWORK_ERROR
    } catch (_: Exception) {
        SignInResult.FAILED
    }

    override suspend fun signOut() {
        auth.signOut()
        // Forget the chosen Google account, so the next sign-in shows the account picker again.
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}

private fun FirebaseUser.toCurrentUser() = CurrentUser(
    userId = uid,
    displayName = displayName?.takeIf { it.isNotBlank() } ?: email?.substringBefore('@') ?: "You",
    email = email,
)
