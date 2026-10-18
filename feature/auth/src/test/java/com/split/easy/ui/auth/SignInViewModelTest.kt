package com.split.easy.ui.auth

import com.split.easy.domain.repository.SignInResult
import com.split.easy.testing.FakeAuthRepository
import com.split.easy.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignInViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val viewModel = SignInViewModel(auth)

    @Test
    fun `a Google token signs in to Firebase`() {
        viewModel.onSignInStarted()
        assertTrue(viewModel.uiState.value.isSigningIn)
        viewModel.onGoogleIdToken("token-123")
        assertEquals(listOf("token-123"), auth.tokens)
        assertEquals("uid-you", auth.currentUser()?.userId)
    }

    @Test
    fun `closing the account picker isn't an error`() {
        viewModel.onSignInStarted()
        viewModel.onSignInCancelled()
        assertEquals(SignInUiState(), viewModel.uiState.value)
    }

    @Test
    fun `Firebase errors are shown and the button comes back`() {
        auth.nextResult = SignInResult.NETWORK_ERROR
        viewModel.onSignInStarted()
        viewModel.onGoogleIdToken("token")
        assertEquals(SignInError.NETWORK, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSigningIn)

        auth.nextResult = SignInResult.FAILED
        viewModel.onGoogleIdToken("token")
        assertEquals(SignInError.FAILED, viewModel.uiState.value.error)
    }

    @Test
    fun `account picker errors are shown`() {
        viewModel.onSignInError(SignInError.NO_ACCOUNT)
        assertEquals(SignInError.NO_ACCOUNT, viewModel.uiState.value.error)
    }
}
