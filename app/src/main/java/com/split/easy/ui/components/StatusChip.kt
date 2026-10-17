package com.split.easy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.split.easy.ui.theme.SplitEasyTheme

/** Small labels such as "Pending", "Not synced", "Guest", "Last used" and the "1 to confirm" badge. */
enum class ChipStyle {
    PRIMARY,
    OWE,
    NEUTRAL,
    SOLID,
}

@Composable
fun StatusChip(text: String, style: ChipStyle, modifier: Modifier = Modifier) {
    val colors = SplitEasyTheme.colors
    val (background, content) = when (style) {
        ChipStyle.PRIMARY -> colors.primaryTint to colors.onPrimaryTint
        ChipStyle.OWE -> colors.oweTint to colors.onOweTint
        ChipStyle.NEUTRAL -> colors.neutralTint to colors.onNeutralTint
        ChipStyle.SOLID -> colors.primary to colors.onPrimary
    }
    Text(
        text = text,
        modifier = modifier
            .background(background, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = content,
    )
}
