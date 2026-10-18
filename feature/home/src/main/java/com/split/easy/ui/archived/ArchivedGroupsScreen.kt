package com.split.easy.ui.archived

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.split.easy.core.designsystem.R
import com.split.easy.ui.components.CardDivider
import com.split.easy.ui.components.GroupAvatarShape
import com.split.easy.ui.components.InitialsAvatar
import com.split.easy.ui.components.OutlinedActionButton
import com.split.easy.ui.components.SplitEasyCard
import com.split.easy.ui.components.SplitEasyTopBar
import com.split.easy.ui.components.avatarTintFor
import com.split.easy.ui.format.formatShortDate
import com.split.easy.ui.format.toLocalDate
import com.split.easy.ui.theme.SplitEasyTheme

@Composable
fun ArchivedGroupsRoute(
    onBack: () -> Unit,
    onOpenGroup: (String) -> Unit,
    viewModel: ArchivedGroupsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ArchivedGroupsScreen(state, onBack, onOpenGroup, viewModel::onUnarchive)
}

/** Archived groups (13): read-only groups; the creator can unarchive (F6). */
@Composable
fun ArchivedGroupsScreen(
    state: ArchivedGroupsUiState,
    onBack: () -> Unit,
    onOpenGroup: (String) -> Unit,
    onUnarchive: (String) -> Unit,
) {
    val colors = SplitEasyTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        SplitEasyTopBar(title = stringResource(R.string.archived_title), onNavigate = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                stringResource(R.string.archived_help),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
            if (state.rows.isEmpty() && !state.isLoading) {
                Text(
                    stringResource(R.string.archived_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }
            SplitEasyCard(Modifier.fillMaxWidth()) {
                state.rows.forEachIndexed { index, row ->
                    if (index > 0) CardDivider()
                    ArchivedRow(row, onOpenGroup, onUnarchive)
                }
            }
        }
    }
}

@Composable
private fun ArchivedRow(
    row: ArchivedGroupRow,
    onOpenGroup: (String) -> Unit,
    onUnarchive: (String) -> Unit,
) {
    val colors = SplitEasyTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onOpenGroup(row.group.id) }
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialsAvatar(row.group.name, shape = GroupAvatarShape, tint = avatarTintFor(row.group.id))
        Column(Modifier.weight(1f)) {
            Text(row.group.name, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
            Text(
                stringResource(
                    R.string.archived_on,
                    formatShortDate(row.group.lastActivityAt.toLocalDate()),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
        if (row.canUnarchive) {
            OutlinedActionButton(stringResource(R.string.archived_unarchive), onClick = {
                onUnarchive(row.group.id)
            })
        } else if (row.creatorName != null) {
            Text(
                stringResource(R.string.archived_creator_can_unarchive, row.creatorName),
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
            )
        }
    }
}
