package com.split.easy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.split.easy.ui.theme.SplitEasyTheme

/** The white card with a thin border used for every list in the designs. */
@Composable
fun SplitEasyCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = SplitEasyTheme.colors.surface,
        border = BorderStroke(1.dp, SplitEasyTheme.colors.border),
    ) {
        Column(content = content)
    }
}

/** The thin line between rows inside a card. */
@Composable
fun CardDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = SplitEasyTheme.colors.divider)
}

/** Small capitals above a card: "NEEDS YOUR CONFIRMATION". Marked as a heading for TalkBack. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    Text(
        text = text.uppercase(locale),
        modifier = modifier
            .padding(top = 20.dp, bottom = 8.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.labelMedium,
        color = SplitEasyTheme.colors.muted,
    )
}
