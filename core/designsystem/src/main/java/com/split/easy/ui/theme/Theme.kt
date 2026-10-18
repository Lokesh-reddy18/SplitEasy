package com.split.easy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.split.easy.domain.model.ThemeSetting

private val LocalSplitEasyColors = staticCompositionLocalOf { LightColors }

@Composable
fun SplitEasyTheme(
    themeSetting: ThemeSetting = ThemeSetting.SYSTEM,
    content: @Composable () -> Unit,
) {
    val colors = if (themeSetting.isDark(isSystemInDarkTheme())) DarkColors else LightColors
    CompositionLocalProvider(LocalSplitEasyColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = SplitEasyTypography,
            shapes = SplitEasyShapes,
            content = content,
        )
    }
}

/** Access to the full token set: `SplitEasyTheme.colors.owe`. */
object SplitEasyTheme {
    val colors: SplitEasyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSplitEasyColors.current
}

fun ThemeSetting.isDark(systemIsDark: Boolean): Boolean = when (this) {
    ThemeSetting.SYSTEM -> systemIsDark
    ThemeSetting.LIGHT -> false
    ThemeSetting.DARK -> true
}

/** Material components (text fields, dialogs, sheets) read these slots. */
private fun SplitEasyColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryTint,
        onPrimaryContainer = onPrimaryTint,
        secondary = onSecondaryTint,
        onSecondary = surface,
        secondaryContainer = secondaryTint,
        onSecondaryContainer = onSecondaryTint,
        background = background,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = neutralTint,
        onSurfaceVariant = muted,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface,
        outline = border,
        outlineVariant = divider,
        // The designs have no red: errors and destructive actions use the "owe" amber.
        error = owe,
        onError = onOwe,
        errorContainer = oweTint,
        onErrorContainer = onOweTint,
    )
}
