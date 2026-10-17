package com.split.easy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.split.easy.domain.model.ThemeSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `theme setting decides dark mode`() {
        assertTrue(ThemeSetting.SYSTEM.isDark(systemIsDark = true))
        assertFalse(ThemeSetting.SYSTEM.isDark(systemIsDark = false))
        assertFalse(ThemeSetting.LIGHT.isDark(systemIsDark = true))
        assertTrue(ThemeSetting.DARK.isDark(systemIsDark = false))
    }

    @Test
    fun `dark setting provides the dark palette to tokens and Material`() {
        var tokens: SplitEasyColors? = null
        var materialBackground = Color.Unspecified
        composeRule.setContent {
            SplitEasyTheme(ThemeSetting.DARK) {
                tokens = SplitEasyTheme.colors
                materialBackground = MaterialTheme.colorScheme.background
            }
        }
        assertEquals(DarkColors, tokens)
        assertEquals(DarkColors.background, materialBackground)
    }

    @Test
    fun `light setting provides the light palette`() {
        var tokens: SplitEasyColors? = null
        var materialError = Color.Unspecified
        composeRule.setContent {
            SplitEasyTheme(ThemeSetting.LIGHT) {
                tokens = SplitEasyTheme.colors
                materialError = MaterialTheme.colorScheme.error
            }
        }
        assertEquals(LightColors, tokens)
        // The designs have no red; errors use the "owe" amber.
        assertEquals(LightColors.owe, materialError)
    }
}
