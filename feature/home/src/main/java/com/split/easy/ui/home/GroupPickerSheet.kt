package com.split.easy.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.split.easy.core.designsystem.R
import com.split.easy.domain.usecase.GroupSummary
import com.split.easy.ui.components.CardDivider
import com.split.easy.ui.components.ChipStyle
import com.split.easy.ui.components.GroupAvatarShape
import com.split.easy.ui.components.InitialsAvatar
import com.split.easy.ui.components.SplitEasyCard
import com.split.easy.ui.components.StatusChip
import com.split.easy.ui.components.TextActionButton
import com.split.easy.ui.components.avatarTintFor
import com.split.easy.ui.theme.SplitEasyTheme

/** "Add expense to" (19): opened by Add expense on Groups home and by the widget's "+". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupPickerSheet(
    groups: List<GroupSummary>,
    lastUsedGroupId: String?,
    onPick: (String) -> Unit,
    onNewGroup: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SplitEasyTheme.colors.surface,
    ) {
        GroupPickerContent(groups, lastUsedGroupId, onPick, onNewGroup)
    }
}

@Composable
fun GroupPickerContent(
    groups: List<GroupSummary>,
    lastUsedGroupId: String?,
    onPick: (String) -> Unit,
    onNewGroup: () -> Unit,
) {
    val colors = SplitEasyTheme.colors
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        Text(
            stringResource(R.string.picker_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        SplitEasyCard {
            groups.forEachIndexed { index, summary ->
                if (index > 0) CardDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(summary.group.id) }
                        .heightIn(min = 64.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    InitialsAvatar(
                        name = summary.group.name,
                        size = 40.dp,
                        shape = GroupAvatarShape,
                        tint = avatarTintFor(summary.group.id),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            summary.group.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.ink,
                        )
                        Text(
                            pluralStringResource(
                                R.plurals.members_count,
                                summary.memberCount,
                                summary.memberCount,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted,
                        )
                    }
                    if (summary.group.id == lastUsedGroupId) {
                        StatusChip(stringResource(R.string.picker_last_used), ChipStyle.PRIMARY)
                    }
                }
            }
        }
        TextActionButton(
            text = stringResource(R.string.home_new_group),
            onClick = onNewGroup,
            icon = R.drawable.ic_add,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp),
        )
    }
}
