package com.split.easy.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.split.easy.domain.repository.AuthRepository
import com.split.easy.domain.repository.SignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why the last sign-in attempt didn't work, shown under the button. */
enum class SignInError {
    /** No internet, from Google's account picker or from Firebase. */
    NETWORK,

    /** The phone has no Google account, or Play services can't show the picker. */
    NO_ACCOUNT,
    FAILED,
}

data class SignInUiState(val isSigningIn: Boolean = false, val error: SignInError? = null)

@HiltViewModel
class SignInViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    /** The account picker is opening. */
    fun onSignInStarted() = _uiState.update { SignInUiState(isSigningIn = true) }

    /** The user closed the account picker: no error, just back to the button. */
    fun onSignInCancelled() = _uiState.update { SignInUiState() }

    fun onSignInError(error: SignInError) = _uiState.update { SignInUiState(error = error) }

    /** Google returned an ID token; finish signing in with Firebase. The app moves on by itself. */
    fun onGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            when (auth.signInWithGoogle(idToken)) {
                SignInResult.SUCCESS -> Unit
                SignInResult.NETWORK_ERROR -> onSignInError(SignInError.NETWORK)
                SignInResult.FAILED -> onSignInError(SignInError.FAILED)
            }
        }
    }
}
