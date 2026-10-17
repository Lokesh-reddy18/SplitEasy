package com.split.easy.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.split.easy.domain.model.ThemeSetting
import com.split.easy.ui.theme.SplitEasyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `amounts are shown in rupees with Indian grouping`() {
        composeRule.setContent {
            SplitEasyTheme(ThemeSetting.LIGHT) {
                AmountText(1_00_000_00)
                AmountText(-1_70_000, signed = true)
            }
        }
        composeRule.onNodeWithText("₹1,00,000").assertIsDisplayed()
        composeRule.onNodeWithText("\u2212₹1,700").assertIsDisplayed()
    }

    @Test
    fun `section header is upper case and marked as a heading`() {
        composeRule.setContent { SplitEasyTheme { SectionHeader("Needs your confirmation") } }
        composeRule.onNodeWithText("NEEDS YOUR CONFIRMATION")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun `disabled primary button can't be clicked`() {
        var clicks = 0
        composeRule.setContent {
            SplitEasyTheme { PrimaryButton("Pay", onClick = { clicks++ }, enabled = false) }
        }
        composeRule.onNodeWithText("Pay").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun `top bar back button has a TalkBack label and calls back`() {
        var backs = 0
        composeRule.setContent {
            SplitEasyTheme {
                SplitEasyTopBar(title = "Balances", subtitle = "Coorg trip", onNavigate = {
                    backs++
                })
            }
        }
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
        composeRule.onNodeWithText("Coorg trip").assertIsDisplayed()
    }
}
