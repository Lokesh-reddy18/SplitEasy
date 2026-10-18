package com.split.easy.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.split.easy.R
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
@Composable
fun GroupRow(summary: GroupSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SplitEasyTheme.colors
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
        }
        BalanceLabel(summary.myNetPaise)
    }
}

/** "you are owed ₹1,850", "you owe ₹600" or "settled up". */
@Composable
private fun BalanceLabel(netPaise: Long) {
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
    Column(horizontalAlignment = Alignment.End) {
        Text(
            stringResource(if (owed) R.string.group_row_owed else R.string.group_row_owe),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = colors.muted,
            textAlign = TextAlign.End,
        )
        AmountText(
            paise = abs(netPaise),
            style = MaterialTheme.typography.bodyLarge,
            color = if (owed) colors.primaryText else colors.owe,
        )
    }
}
