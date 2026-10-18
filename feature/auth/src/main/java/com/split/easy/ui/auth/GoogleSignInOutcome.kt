package com.split.easy.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** What Google's account picker returned. */
sealed interface GoogleSignInOutcome {
    data class Token(val idToken: String) : GoogleSignInOutcome

    data object Cancelled : GoogleSignInOutcome

    data class Error(val error: SignInError) : GoogleSignInOutcome
}

/**
 * Shows Google's account picker through Credential Manager and returns the account's ID token.
 * [webClientId] is the "Web client" OAuth ID from Firebase, which Firebase Auth checks the token against.
 */
suspend fun requestGoogleIdToken(context: Context, webClientId: String): GoogleSignInOutcome {
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
        .build()
    return try {
        val credential = CredentialManager.create(
            context,
        ).getCredential(context, request).credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleSignInOutcome.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            GoogleSignInOutcome.Error(SignInError.FAILED)
        }
    } catch (_: GetCredentialCancellationException) {
        GoogleSignInOutcome.Cancelled
    } catch (_: NoCredentialException) {
        GoogleSignInOutcome.Error(SignInError.NO_ACCOUNT)
    } catch (e: GetCredentialException) {
        val network = e.type.contains("NETWORK", ignoreCase = true)
        GoogleSignInOutcome.Error(if (network) SignInError.NETWORK else SignInError.FAILED)
    }
}
