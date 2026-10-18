package com.split.easy.ui.balances

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.split.easy.R
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.money.formatInr
import com.split.easy.ui.components.AmountText
import com.split.easy.ui.components.CardDivider
import com.split.easy.ui.components.ChipStyle
import com.split.easy.ui.components.InitialsAvatar
import com.split.easy.ui.components.OutlinedActionButton
import com.split.easy.ui.components.PrimaryButton
import com.split.easy.ui.components.SectionHeader
import com.split.easy.ui.components.SplitEasyCard
import com.split.easy.ui.components.SplitEasyTopBar
import com.split.easy.ui.components.StatusChip
import com.split.easy.ui.theme.SplitEasyTheme

@Composable
fun BalancesRoute(
    onBack: () -> Unit,
    onPay: (groupId: String, toMemberId: String) -> Unit,
    viewModel: BalancesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BalancesScreen(
        state = state,
        onBack = onBack,
        onConfirm = viewModel::onConfirm,
        onReject = viewModel::onReject,
        onCancel = viewModel::onCancel,
        onPay = { onPay(viewModel.groupId, it) },
    )
}

/** Balances (05 for the payee, 22 for someone who owes). */
@Composable
fun BalancesScreen(
    state: BalancesUiState,
    onBack: () -> Unit,
    onConfirm: (String) -> Unit,
    onReject: (String) -> Unit,
    onCancel: (String) -> Unit,
    onPay: (toMemberId: String) -> Unit,
) {
    val colors = SplitEasyTheme.colors
    val you = stringResource(R.string.you)
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        SplitEasyTopBar(
            title = stringResource(R.string.balances_title),
            subtitle = state.groupName,
            onNavigate = onBack,
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            if (state.toConfirm.isNotEmpty()) {
                SectionHeader(stringResource(R.string.balances_needs_confirmation))
                state.toConfirm.forEach { row ->
                    ConfirmCard(row, onConfirm, onReject)
                }
            }
            if (state.waiting.isNotEmpty()) {
                SectionHeader(stringResource(R.string.balances_waiting))
                SplitEasyCard(Modifier.fillMaxWidth()) {
                    state.waiting.forEachIndexed { i, row ->
                        if (i > 0) CardDivider()
                        WaitingRow(row, onCancel)
                    }
                }
            }

            SectionHeader(stringResource(R.string.balances_suggested))
            if (state.suggestions.isEmpty()) {
                Text(
                    stringResource(R.string.balances_all_settled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            } else {
                Text(
                    pluralStringResource(
                        R.plurals.balances_settle_count,
                        state.suggestions.size,
                        state.suggestions.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 0.dp, bottom = 8.dp),
                )
                SplitEasyCard(Modifier.fillMaxWidth()) {
                    state.suggestions.forEachIndexed { i, row ->
                        if (i > 0) CardDivider()
                        SuggestionRowView(row, you, onPay)
                    }
                }
            }

            if (state.net.isNotEmpty()) {
                SectionHeader(stringResource(R.string.balances_net))
                SplitEasyCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        state.net.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                pair.forEach { NetCell(it, you, Modifier.weight(1f)) }
                                if (pair.size == 1) Text("", Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The violet-bordered card: "Ravi paid you ₹1,200 · via UPI · Ref …" with Not received / Confirm. */
@Composable
private fun ConfirmCard(row: PendingRow, onConfirm: (String) -> Unit, onReject: (String) -> Unit) {
    val colors = SplitEasyTheme.colors
    val s = row.settlement
    val from = row.fromName.orEmpty()
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.surface,
        border = BorderStroke(2.dp, colors.primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InitialsAvatar(from)
                Column {
                    Text(
                        stringResource(R.string.balances_paid_you, from, formatInr(s.amountPaise)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.ink,
                    )
                    Text(
                        methodLine(s.method, s.upiTxnId),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                    )
                }
            }
            // Wording from OPEN_QUESTIONS.md section G.
            val message = if (s.method == SettlementMethod.UPI) {
                stringResource(R.string.balances_upi_message, from)
            } else {
                stringResource(R.string.balances_cash_message, from, formatInr(s.amountPaise))
            }
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onNeutralTint,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (row.canAct) {
                Row(
                    Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedActionButton(
                        stringResource(R.string.balances_not_received),
                        onClick = { onReject(s.id) },
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(stringResource(R.string.balances_confirm), onClick = {
                        onConfirm(s.id)
                    }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun methodLine(method: SettlementMethod, ref: String?): String = when {
    method == SettlementMethod.CASH -> stringResource(R.string.balances_in_cash)
    ref != null -> stringResource(R.string.balances_via_upi_ref, formatUpiRef(ref))
    else -> stringResource(R.string.balances_via_upi)
}

/** "You paid Ananya ₹1,200 · via UPI · waiting for Ananya to confirm" with Cancel (22). */
@Composable
private fun WaitingRow(row: PendingRow, onCancel: (String) -> Unit) {
    val colors = SplitEasyTheme.colors
    val s = row.settlement
    val to = row.toName.orEmpty()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialsAvatar(to, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.balances_you_paid, to, formatInr(s.amountPaise)),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
            )
            Text(
                stringResource(R.string.balances_waiting_for, methodLine(s.method, null), to),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
        if (row.canAct) {
            OutlinedActionButton(stringResource(R.string.balances_cancel), onClick = {
                onCancel(s.id)
            }, contentColor = colors.owe)
        }
    }
}

/** "Ravi → You · Pending · ₹1,200", with Pay or Waiting on your own debts. */
@Composable
private fun SuggestionRowView(row: SuggestionRow, you: String, onPay: (String) -> Unit) {
    val colors = SplitEasyTheme.colors
    val from = row.fromName ?: you
    val to = row.toName ?: you
    val spoken =
        stringResource(R.string.balances_payment_from_to, from, to, formatInr(row.amountPaise))
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier
                .weight(1f)
                .clearAndSetSemantics { contentDescription = spoken },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(from, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
            Icon(
                painterResource(R.drawable.ic_arrow_right),
                null,
                tint = colors.muted,
                modifier = Modifier.size(18.dp),
            )
            Text(
                to,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            if (row.hasPending &&
                !row.isMine
            ) {
                StatusChip(stringResource(R.string.feed_pending), ChipStyle.PRIMARY)
            }
            AmountText(
                row.amountPaise,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
            )
        }
        when {
            row.isMine && row.payablePaise > 0 ->
                PrimaryButton(stringResource(R.string.balances_pay), onClick = {
                    onPay(row.toMemberId)
                })

            row.isMine && row.hasPending -> StatusChip(
                stringResource(R.string.balances_waiting_chip),
                ChipStyle.PRIMARY,
            )
        }
    }
}

@Composable
private fun NetCell(row: NetRow, you: String, modifier: Modifier) {
    val colors = SplitEasyTheme.colors
    Row(
        modifier
            .heightIn(min = 40.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            row.name ?: you,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        AmountText(
            row.netPaise,
            signed = true,
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                row.netPaise > 0 -> colors.primaryText
                row.netPaise < 0 -> colors.owe
                else -> colors.muted
            },
        )
    }
}
