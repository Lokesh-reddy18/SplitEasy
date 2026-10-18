package com.split.easy.ui.creategroup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.split.easy.testing.FakeCurrentUser
import com.split.easy.testing.FakeGroupRepository
import com.split.easy.testing.MainDispatcherRule
import com.split.easy.ui.theme.SplitEasyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CreateGroupScreenTest {

    @get:Rule(order = 0)
    val mainDispatcher = MainDispatcherRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun `typing a name enables Create, and guests can be added and removed`() {
        val viewModel = CreateGroupViewModel(FakeGroupRepository(), FakeCurrentUser())
        composeRule.setContent {
            SplitEasyTheme { CreateGroupRoute(onClose = {}, onCreated = {}, viewModel = viewModel) }
        }

        composeRule.onNodeWithText("Create").assertIsNotEnabled()
        composeRule.onNodeWithText("e.g. Coorg trip").performTextInput("Coorg trip")
        composeRule.onNodeWithText("Create").assertIsEnabled()

        composeRule.onNodeWithText("Name, e.g. Ravi").performTextInput("Priya")
        composeRule.onNodeWithText("Add").performClick()
        composeRule.onNodeWithText("Priya").assertIsDisplayed()
        composeRule.onNodeWithText("2 people · you can add more later").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Remove Priya").performClick()
        composeRule.onNodeWithText("Priya").assertDoesNotExist()
    }
}
