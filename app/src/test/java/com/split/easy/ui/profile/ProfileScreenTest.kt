package com.split.easy.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    private val user = CurrentUser("local-user", "You", isPlaceholder = true)

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
}
