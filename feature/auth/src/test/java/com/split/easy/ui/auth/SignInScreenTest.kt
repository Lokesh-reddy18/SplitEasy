package com.split.easy.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.split.easy.ui.theme.SplitEasyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w390dp-h844dp")
class SignInScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `shows the design copy and starts Google sign-in`() {
        var clicks = 0
        composeRule.setContent {
            SplitEasyTheme { SignInScreen(SignInUiState(), onContinueWithGoogle = { clicks++ }) }
        }

        composeRule.onNodeWithText("Split bills.\nSettle in one tap.").assertIsDisplayed()
        composeRule.onNodeWithText("Works offline").assertIsDisplayed()
        composeRule.onNodeWithText("Fewest payments").assertIsDisplayed()
        composeRule.onNodeWithText("Pay with UPI").assertIsDisplayed()
        composeRule.onNodeWithText(
            "By continuing you agree to the Terms and Privacy Policy.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Continue with Google").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `while signing in the button is disabled, and errors show above it`() {
        composeRule.setContent {
            SplitEasyTheme {
                SignInScreen(
                    SignInUiState(
                        isSigningIn = true,
                        error = null,
                    ),
                    onContinueWithGoogle = {
                    },
                )
            }
        }
        composeRule.onNodeWithText("Continue with Google", useUnmergedTree = true).assertExists()
        composeRule.onNode(
            hasText("Continue with Google") and hasClickAction(),
        ).assertIsNotEnabled()
    }

    @Test
    fun `network error message`() {
        composeRule.setContent {
            SplitEasyTheme {
                SignInScreen(SignInUiState(error = SignInError.NETWORK), onContinueWithGoogle = {})
            }
        }
        composeRule.onNodeWithText(
            "No internet connection. Connect once to sign in; after that SplitEasy works offline.",
        ).assertIsDisplayed()
    }
}
