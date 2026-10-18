package com.split.easy.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.split.easy.domain.model.CurrentUser
import com.split.easy.domain.model.ThemeSetting
import com.split.easy.ui.theme.SplitEasyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w390dp-h844dp")
class ProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val user = CurrentUser("uid-ananya", "Ananya Rao", email = "ananya.rao@gmail.com")

    @Test
    fun `shows the UPI ID and theme, without the P2 rows`() {
        composeRule.setContent {
            SplitEasyTheme {
                ProfileScreen(ProfileUiState(user, upiId = "ananya.r@okaxis"), {
                }, {}, {}, showDeveloperOptions = false)
            }
        }
        composeRule.onNodeWithText("ananya.r@okaxis · friends pay you here").assertIsDisplayed()
        composeRule.onNodeWithText("System default").assertIsDisplayed()
        composeRule.onNodeWithText("Reminders").assertDoesNotExist()
        composeRule.onNodeWithText("Export data").assertDoesNotExist()
        composeRule.onNodeWithText("Load demo data").assertDoesNotExist()
    }

    @Test
    fun `choosing a theme saves it`() {
        val picked = mutableListOf<ThemeSetting>()
        composeRule.setContent {
            SplitEasyTheme {
                ProfileScreen(ProfileUiState(user), {
                }, { picked += it }, {}, showDeveloperOptions = true)
            }
        }
        composeRule.onNodeWithText("Add one so friends can pay you").assertIsDisplayed()
        composeRule.onNodeWithText("Theme").performClick()
        composeRule.onNodeWithText("Dark").performClick()
        assertEquals(listOf(ThemeSetting.DARK), picked)
    }

    @Test
    fun `saving a UPI ID from Profile`() {
        val saved = mutableListOf<String>()
        composeRule.setContent {
            SplitEasyTheme {
                ProfileScreen(ProfileUiState(user), {
                    saved += it
                }, {}, {}, showDeveloperOptions = false)
            }
        }
        composeRule.onNodeWithText("Add").performClick()
        composeRule.onNodeWithText("Your UPI ID").assertIsDisplayed()
    }

    @Test
    fun `shows the Google account and signs out`() {
        var signOuts = 0
        composeRule.setContent {
            SplitEasyTheme {
                ProfileScreen(ProfileUiState(user), {
                }, {}, {}, showDeveloperOptions = false, onSignOut = { signOuts++ })
            }
        }
        composeRule.onNodeWithText("Ananya Rao").assertIsDisplayed()
        composeRule.onNodeWithText("ananya.rao@gmail.com").assertIsDisplayed()
        composeRule.onNodeWithText("Sign out").performScrollTo().performClick()
        assertEquals(1, signOuts)
    }

    @Test
    fun `warns before deleting unsynced changes`() {
        var confirmed = 0
        composeRule.setContent {
            SplitEasyTheme {
                SignOutWarningDialog(unsyncedChanges = 3, onConfirm = {
                    confirmed++
                }, onDismiss = {})
            }
        }
        composeRule.onNodeWithText(
            "3 changes haven't synced yet. Signing out deletes them from this phone and they will be lost.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Sign out anyway").performClick()
        assertEquals(1, confirmed)
    }
}
