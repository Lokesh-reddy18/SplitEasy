package com.split.easy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.split.easy.ui.theme.SplitEasyTheme

/** Every button is at least 48 dp tall (accessibility requirement), with 12 dp corners. */
private val MinButtonHeight = 48.dp
private val ButtonPadding = PaddingValues(horizontal = 18.dp)

@Composable
private fun ButtonContent(text: String, @DrawableRes icon: Int?) {
    if (icon != null) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, style = MaterialTheme.typography.labelLarge)
}

/** Filled violet button: Save, Settle up, Confirm, Pay. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    val colors = SplitEasyTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.border,
            disabledContentColor = colors.muted,
        ),
        contentPadding = ButtonPadding,
    ) { ButtonContent(text, icon) }
}

/** Grey filled button: Balances. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SplitEasyTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.neutralTint,
            contentColor = colors.ink,
            disabledContainerColor = colors.neutralTint,
            disabledContentColor = colors.muted,
        ),
        contentPadding = ButtonPadding,
    ) { ButtonContent(text, icon = null) }
}

/** White button with a border: Not received, Cancel, Unarchive. */
@Composable
fun OutlinedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = SplitEasyTheme.colors.ink,
) {
    val colors = SplitEasyTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, colors.border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = contentColor,
            disabledContentColor = colors.muted,
        ),
        contentPadding = ButtonPadding,
    ) { ButtonContent(text, icon = null) }
}

/**
 * Amber button for destructive actions: Delete, Sign out.
 * [filled] is for the confirming button inside a dialog; otherwise it's outlined (as in 09 and 14).
 */
@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    val colors = SplitEasyTheme.colors
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = MinButtonHeight),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.owe,
                contentColor = colors.onOwe,
            ),
            contentPadding = ButtonPadding,
        ) { ButtonContent(text, icon = null) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = MinButtonHeight),
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, colors.owe.copy(alpha = 0.35f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = colors.surface,
                contentColor = colors.owe,
            ),
            contentPadding = ButtonPadding,
        ) { ButtonContent(text, icon = null) }
    }
}

/** Text-only violet action: New group, Edit, Rename, Record payment. */
@Composable
fun TextActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    color: Color = SplitEasyTheme.colors.primaryText,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinButtonHeight),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = color),
    ) { ButtonContent(text, icon) }
}
