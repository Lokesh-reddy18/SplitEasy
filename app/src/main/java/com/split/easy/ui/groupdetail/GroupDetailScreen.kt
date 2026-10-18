package com.split.easy.ui.groupdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.split.easy.R
import com.split.easy.domain.money.formatInr
import com.split.easy.ui.components.CardDivider
import com.split.easy.ui.components.DestructiveButton
import com.split.easy.ui.components.OutlinedActionButton
import com.split.easy.ui.components.PrimaryButton
import com.split.easy.ui.components.SecondaryButton
import com.split.easy.ui.components.SectionHeader
import com.split.easy.ui.components.SplitEasyCard
import com.split.easy.ui.components.SplitEasyTopBar
import com.split.easy.ui.components.TextActionButton
import com.split.easy.ui.format.formatDayHeader
import com.split.easy.ui.format.formatShortDate
import com.split.easy.ui.theme.FabShape
import com.split.easy.ui.theme.SplitEasyTheme
import com.split.easy.ui.upi.UpiIdSheet
import com.split.easy.ui.upi.UpiIdTarget
import kotlin.math.abs

@Composable
fun GroupDetailRoute(
    onBack: () -> Unit,
    onOpenSettings: (String) -> Unit,
    onOpenBalances: (String) -> Unit,
    onSettleUp: (groupId: String, toMemberId: String) -> Unit,
    onAddExpense: (String) -> Unit,
    onOpenExpense: (String) -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val groupId = viewModel.groupId
    GroupDetailScreen(
        state = state,
        onBack = onBack,
        onOpenSettings = { onOpenSettings(groupId) },
        onSettleUp = {
            when (val target = state.settleUpTarget) {
                is SettleUpTarget.Pay -> onSettleUp(groupId, target.toMemberId)
                SettleUpTarget.Balances -> onOpenBalances(groupId)
            }
        },
        onOpenBalances = { onOpenBalances(groupId) },
        onAddExpense = { onAddExpense(groupId) },
        onOpenExpense = onOpenExpense,
        onUndoSettlement = viewModel::onUndoSettlement,
        onDismissUpiPrompt = viewModel::onDismissUpiPrompt,
        onSaveMyUpiId = viewModel::onSaveMyUpiId,
    )
}

/** Group detail (03): your balance, Settle up and Balances, and the feed grouped by day. */
@Composable
fun GroupDetailScreen(
    state: GroupDetailUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSettleUp: () -> Unit,
    onOpenBalances: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenExpense: (String) -> Unit,
    onUndoSettlement: (String) -> Unit,
    onDismissUpiPrompt: () -> Unit = {},
    onSaveMyUpiId: (String) -> Unit = {},
) {
    val colors = SplitEasyTheme.colors
    val you = stringResource(R.string.you)
    var undoTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var editUpi by rememberSaveable { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(Modifier.fillMaxSize()) {
            SplitEasyTopBar(
                title = state.groupName,
                subtitle = state.memberNames.joinToString { it ?: you },
                onNavigate = onBack,
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painterResource(R.drawable.ic_more),
                            contentDescription = stringResource(R.string.group_settings),
                            tint = colors.ink,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
            )
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 104.dp,
                ),
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding(),
            ) {
                item { BalanceCard(state, onSettleUp, onOpenBalances) }
                if (state.showUpiPrompt) {
                    item {
                        UpiPromptCard(state.groupName, onAdd = {
                            editUpi = true
                        }, onNotNow = onDismissUpiPrompt)
                    }
                }
                if (state.feed.isEmpty() && !state.isLoading) {
                    item {
                        Text(
                            stringResource(R.string.group_feed_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }
                items(state.feed, key = { it.date.toString() }) { day ->
                    SectionHeader(formatDayHeader(day.date))
                    SplitEasyCard(Modifier.fillMaxWidth()) {
                        day.items.forEachIndexed { index, item ->
                            if (index > 0) CardDivider()
                            when (item) {
                                is FeedItem.ExpenseItem -> ExpenseRow(item, onClick = {
                                    onOpenExpense(item.expense.id)
                                })

                                is FeedItem.SettlementItem -> SettlementRow(
                                    item,
                                    onClick = if (item.canUndo) {
                                        { undoTarget = item.settlement.id }
                                    } else {
                                        null
                                    },
                                )

                                is FeedItem.EventItem -> EventRow(item)
                            }
                        }
                    }
                }
            }
        }
        if (state.canChangeThings) {
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
                    .navigationBarsPadding()
                    .padding(20.dp),
            )
        }
    }

    if (editUpi) {
        UpiIdSheet(
            target = UpiIdTarget.Mine,
            current = null,
            onSave = {
                onSaveMyUpiId(it)
                editUpi = false
            },
            onDismiss = { editUpi = false },
        )
    }

    val settlementToUndo = undoTarget?.let { id ->
        state.feed.flatMap { it.items }.filterIsInstance<FeedItem.SettlementItem>().firstOrNull {
            it.settlement.id ==
                id
        }
    }
    if (settlementToUndo != null) {
        UndoSettlementDialog(
            item = settlementToUndo,
            onConfirm = {
                onUndoSettlement(settlementToUndo.settlement.id)
                undoTarget = null
            },
            onDismiss = { undoTarget = null },
        )
    }
}

@Composable
private fun BalanceCard(
    state: GroupDetailUiState,
    onSettleUp: () -> Unit,
    onOpenBalances: () -> Unit,
) {
    val colors = SplitEasyTheme.colors
    SplitEasyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(
                stringResource(R.string.group_balance_label),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
            val net = state.myNetPaise
            val (text, color) = when {
                net > 0 -> stringResource(R.string.group_balance_owed, formatInr(net)) to
                    colors.primaryText

                net < 0 -> stringResource(R.string.group_balance_owe, formatInr(abs(net))) to
                    colors.owe

                else -> stringResource(R.string.group_balance_settled) to colors.ink
            }
            Text(
                text,
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                modifier = Modifier.padding(top = 2.dp),
            )
            when {
                state.isArchived -> Note(stringResource(R.string.group_archived_note))

                !state.isMember -> Note(stringResource(R.string.group_left_note))

                else -> Row(
                    Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PrimaryButton(
                        stringResource(R.string.group_settle_up),
                        onClick = onSettleUp,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        stringResource(R.string.group_balances),
                        onClick = onOpenBalances,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = SplitEasyTheme.colors.muted,
        modifier = Modifier.padding(top = 10.dp),
    )
}

/** Confirmation before undoing a confirmed payment (15, Q6). */
@Composable
private fun UndoSettlementDialog(
    item: FeedItem.SettlementItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val settlement = item.settlement
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SplitEasyTheme.colors.surface,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                stringResource(R.string.undo_settlement_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                stringResource(
                    R.string.undo_settlement_message,
                    settlementTitle(item),
                    formatInr(settlement.amountPaise),
                    formatShortDate(settlement.date),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        dismissButton = {
            OutlinedActionButton(stringResource(R.string.action_cancel), onClick = onDismiss)
        },
        confirmButton = {
            DestructiveButton(
                stringResource(R.string.undo_settlement_confirm),
                onClick = onConfirm,
                filled = true,
            )
        },
    )
}

/** First-time UPI ID prompt (18): shown when you're owed money and have no UPI ID. */
@Composable
private fun UpiPromptCard(groupName: String, onAdd: () -> Unit, onNotNow: () -> Unit) {
    val colors = SplitEasyTheme.colors
    Row(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .background(colors.primaryTint, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(colors.surface, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_upi_card),
                contentDescription = null,
                tint = colors.onPrimaryTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.upi_prompt_title),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
            )
            Text(
                stringResource(R.string.upi_prompt_body, groupName),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onNeutralTint,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(stringResource(R.string.upi_prompt_add), onClick = onAdd)
                TextActionButton(
                    stringResource(R.string.upi_prompt_not_now),
                    onClick = onNotNow,
                    color = colors.onPrimaryTint,
                )
            }
        }
    }
}
