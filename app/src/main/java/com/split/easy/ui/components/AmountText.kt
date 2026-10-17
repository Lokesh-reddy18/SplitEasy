package com.split.easy.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.split.easy.domain.money.formatInr
import com.split.easy.domain.money.formatInrSigned
import com.split.easy.ui.theme.TABULAR_NUMBERS

/**
 * Shows paise as rupees through [formatInr], the only way money reaches the screen.
 * Uses tabular numbers so amounts line up in lists. [signed] adds "+" for net balances.
 */
@Composable
fun AmountText(
    paise: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = FontWeight.Bold,
    signed: Boolean = false,
) {
    Text(
        text = if (signed) formatInrSigned(paise) else formatInr(paise),
        modifier = modifier,
        style = style.copy(fontFeatureSettings = TABULAR_NUMBERS),
        color = color,
        fontWeight = fontWeight,
    )
}
