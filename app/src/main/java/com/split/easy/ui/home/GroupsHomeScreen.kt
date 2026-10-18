package com.split.easy.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.split.easy.R
import com.split.easy.config.FeatureFlags
import com.split.easy.domain.model.CurrentUser
import com.split.easy.domain.usecase.GroupSummary
import com.split.easy.ui.components.AmountText
import com.split.easy.ui.components.CardDivider
import com.split.easy.ui.components.InitialsAvatar
import com.split.easy.ui.components.ProfileIconAvatar
import com.split.easy.ui.components.SplitEasyCard
import com.split.easy.ui.components.TextActionButton
import com.split.easy.ui.theme.FabShape
import com.split.easy.ui.theme.SplitEasyTheme
import kotlin.math.abs

@Composable
fun GroupsHomeRoute(
    onOpenGroup: (String) -> Unit,
    onNewGroup: () -> Unit,
    onAddExpense: (String) -> Unit,
    onOpenArchived: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPicker by rememberSaveable { mutableStateOf(false) }

    GroupsHomeScreen(
        state = state,
        onOpenGroup = onOpenGroup,
        onNewGroup = onNewGroup,
        onAddExpense = { showPicker = true },
        onOpenArchived = onOpenArchived,
        onOpenProfile = onOpenProfile,
    )
    if (showPicker) {
        GroupPickerSheet(
            groups = state.pickerGroups,
            lastUsedGroupId = state.lastUsedGroupId,
            onPick = { groupId ->
                showPicker = false
                viewModel.onGroupPicked(groupId)
                onAddExpense(groupId)
            },
            onNewGroup = {
                showPicker = false
                onNewGroup()
            },
            onDismiss = { showPicker = false },
        )
    }
}

/** Groups home (02): totals card, your groups, and Add expense. */
@Composable
fun GroupsHomeScreen(
    state: HomeUiState,
    onOpenGroup: (String) -> Unit,
    onNewGroup: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenArchived: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val colors = SplitEasyTheme.colors
    val groups = state.summary.groups
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
        ) {
            item { HomeHeader(state.user, onOpenProfile) }
            if (FeatureFlags.SHOW_SYNC_STATUS && state.pendingChanges > 0) {
                item { OfflineBanner(state.pendingChanges) }
            }
            item {
                SummaryCard(
                    netPaise = state.summary.netPaise,
                    owePaise = state.summary.owePaise,
                    owedPaise = state.summary.owedPaise,
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.home_your_groups),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.ink,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() },
                    )
                    TextActionButton(stringResource(R.string.home_new_group), onClick = onNewGroup)
                }
            }
            if (groups.isEmpty() && !state.isLoading) {
                item { EmptyGroups() }
            } else {
                item { GroupList(groups, onOpenGroup) }
            }
            if (state.archivedCount > 0) {
                item {
                    TextActionButton(
                        text = pluralStringResource(
                            R.plurals.archived_groups_link,
                            state.archivedCount,
                            state.archivedCount,
                        ),
                        onClick = onOpenArchived,
                        color = colors.muted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        if (groups.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onAddExpense,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = {
                    Text(
                        stringResource(R.string.home_add_expense),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                shape = FabShape,
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
            )
        }
    }
}

@Composable
private fun HomeHeader(user: CurrentUser, onOpenProfile: () -> Unit) {
    val colors = SplitEasyTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .background(colors.primary, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_add),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f)
                .semantics { heading() },
        )
        IconButton(onClick = onOpenProfile, modifier = Modifier.size(48.dp)) {
            if (user.isPlaceholder) {
                // No real name before sign-in (M4), so show the profile icon instead of initials.
                ProfileIconAvatar(contentDescription = stringResource(R.string.home_profile_button))
            } else {
                InitialsAvatar(user.displayName)
            }
        }
    }
}

@Composable
private fun OfflineBanner(pendingChanges: Int) {
    val colors = SplitEasyTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(colors.oweTint, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_offline),
            null,
            tint = colors.onOweTint,
            modifier = Modifier.size(16.dp),
        )
        Text(
            pluralStringResource(R.plurals.home_offline_banner, pendingChanges, pendingChanges),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onOweTint,
        )
    }
}

/** The dark "Overall" card: your net across active groups, then what you owe and are owed. */
@Composable
private fun SummaryCard(netPaise: Long, owePaise: Long, owedPaise: Long) {
    val colors = SplitEasyTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.summaryCard, MaterialTheme.shapes.large)
            .padding(20.dp),
    ) {
        val label = when {
            netPaise > 0 -> R.string.home_overall_owed
            netPaise < 0 -> R.string.home_overall_owe
            else -> R.string.home_overall_settled
        }
        Text(
            stringResource(label),
            style = MaterialTheme.typography.bodySmall,
            color = colors.summaryMuted,
        )
        AmountText(
            paise = abs(netPaise),
            style = MaterialTheme.typography.displaySmall,
            color = colors.onSummary,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryTile(
                stringResource(R.string.home_you_owe),
                owePaise,
                colors.summaryOwe,
                Modifier.weight(1f),
            )
            SummaryTile(
                stringResource(R.string.home_you_are_owed),
                owedPaise,
                colors.summaryOwed,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryTile(label: String, paise: Long, amountColor: Color, modifier: Modifier) {
    val colors = SplitEasyTheme.colors
    Column(
        modifier
            .background(colors.summaryTile, MaterialTheme.shapes.small)
            .padding(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.summaryMuted)
        AmountText(
            paise = paise,
            style = MaterialTheme.typography.titleSmall,
            color = amountColor,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun GroupList(groups: List<GroupSummary>, onOpenGroup: (String) -> Unit) {
    if (groups.isEmpty()) return
    SplitEasyCard(Modifier.fillMaxWidth()) {
        groups.forEachIndexed { index, summary ->
            if (index > 0) CardDivider()
            GroupRow(summary, onClick = { onOpenGroup(summary.group.id) })
        }
    }
}

@Composable
private fun EmptyGroups() {
    val colors = SplitEasyTheme.colors
    SplitEasyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.home_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        }
    }
}
