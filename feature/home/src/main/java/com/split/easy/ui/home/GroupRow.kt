package com.split.easy.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.split.easy.core.designsystem.R
import com.split.easy.domain.usecase.GroupSummary
import com.split.easy.ui.components.AmountText
import com.split.easy.ui.components.ChipStyle
import com.split.easy.ui.components.GroupAvatarShape
import com.split.easy.ui.components.InitialsAvatar
import com.split.easy.ui.components.StatusChip
import com.split.easy.ui.components.avatarTintFor
import com.split.easy.ui.theme.SplitEasyTheme
import kotlin.math.abs

/** A group on Groups home: avatar, name, members, "1 to confirm" badge, and your balance. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupRow(summary: GroupSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SplitEasyTheme.colors
    // Large fonts (1.5x and up) stack the balance under the name instead of squeezing both.
    val stacked = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialsAvatar(
            name = summary.group.name,
            shape = GroupAvatarShape,
            tint = avatarTintFor(summary.group.id),
        )
        Column(Modifier.weight(1f)) {
            Text(summary.group.name, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    pluralStringResource(
                        R.plurals.members_count,
                        summary.memberCount,
                        summary.memberCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
                if (summary.waitingForMe > 0) {
                    StatusChip(
                        pluralStringResource(
                            R.plurals.to_confirm_count,
                            summary.waitingForMe,
                            summary.waitingForMe,
                        ),
                        ChipStyle.SOLID,
                    )
                }
            }
            if (stacked) BalanceLabel(summary.myNetPaise, alignEnd = false)
        }
        if (!stacked) BalanceLabel(summary.myNetPaise, alignEnd = true)
    }
}

private const val LARGE_FONT_SCALE = 1.5f

/** "you are owed ₹1,850", "you owe ₹600" or "settled up". */
@Composable
private fun BalanceLabel(netPaise: Long, alignEnd: Boolean) {
    val colors = SplitEasyTheme.colors
    if (netPaise == 0L) {
        Text(
            stringResource(R.string.group_row_settled),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.muted,
        )
        return
    }
    val owed = netPaise > 0
    // Capped so large text wraps here instead of squeezing the group name.
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            stringResource(if (owed) R.string.group_row_owed else R.string.group_row_owe),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = colors.muted,
        )
        AmountText(
            paise = abs(netPaise),
            style = MaterialTheme.typography.bodyLarge,
            color = if (owed) colors.primaryText else colors.owe,
        )
    }
}
