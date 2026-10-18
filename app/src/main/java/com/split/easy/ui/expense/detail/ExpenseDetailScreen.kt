package com.split.easy.ui.expense.detail

import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.split.easy.R
import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.SplitType
import com.split.easy.domain.money.formatInr
import com.split.easy.domain.split.formatBasisPoints
import com.split.easy.ui.components.AmountText
import com.split.easy.ui.components.CardDivider
import com.split.easy.ui.components.CategoryIcon
import com.split.easy.ui.components.DestructiveButton
import com.split.easy.ui.components.OutlinedActionButton
import com.split.easy.ui.components.PrimaryButton
import com.split.easy.ui.components.SectionHeader
import com.split.easy.ui.components.SplitEasyCard
import com.split.easy.ui.components.SplitEasyTopBar
import com.split.easy.ui.components.style
import com.split.easy.ui.format.formatDayHeader
import com.split.easy.ui.format.formatShortDate
import com.split.easy.ui.format.formatTime
import com.split.easy.ui.format.toLocalDate
import com.split.easy.ui.theme.SplitEasyTheme

@Composable
fun ExpenseDetailRoute(
    onBack: () -> Unit,
    onEdit: (groupId: String, expenseId: String) -> Unit,
    viewModel: ExpenseDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.deletedEvents.collect { onBack() } }
    ExpenseDetailScreen(
        state = state,
        onBack = onBack,
        onEdit = { state.expense?.let { onEdit(it.groupId, it.id) } },
        onDelete = viewModel::onDelete,
    )
}

/** Expense detail (14): amount, who paid, each share, who added it; Edit and Delete for its creator. */
@Composable
fun ExpenseDetailScreen(
    state: ExpenseDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = SplitEasyTheme.colors
    val you = stringResource(R.string.you)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val expense = state.expense

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        SplitEasyTopBar(title = stringResource(R.string.detail_title), onNavigate = onBack)
        if (expense == null) return@Column

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            SummaryCard(state, expense)
            SectionHeader(stringResource(R.string.detail_shares))
            SplitEasyCard(Modifier.fillMaxWidth()) {
                state.members.filter {
                    it.id in expense.sharesPaise
                }.forEachIndexed { index, member ->
                    if (index > 0) CardDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            state.nameOf(member.id) ?: you,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.ink,
                        )
                        if (member.id == expense.paidByMemberId) {
                            Text(
                                stringResource(R.string.detail_paid_tag),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                            )
                        }
                        expense.percentBasisPoints[member.id]?.let {
                            Text(
                                "${formatBasisPoints(it)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                            )
                        }
                        Text("", Modifier.weight(1f))
                        AmountText(
                            expense.sharesPaise.getValue(member.id),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.ink,
                        )
                    }
                }
            }
            val addedAt = expense.createdAt
            Text(
                stringResource(
                    R.string.detail_added_by,
                    state.creatorName ?: stringResource(R.string.you_lowercase),
                    "${formatShortDate(addedAt.toLocalDate())}, ${formatTime(addedAt)}",
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = colors.muted,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp),
            )
        }

        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = colors.border)
            if (state.canEdit) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DestructiveButton(stringResource(R.string.detail_delete), onClick = {
                        confirmDelete =
                            true
                    }, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        stringResource(R.string.detail_edit),
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Text(
                    stringResource(
                        R.string.detail_only_creator,
                        state.creatorName ?: stringResource(R.string.detail_someone),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                )
            }
        }
    }

    if (confirmDelete && expense != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = colors.surface,
            shape = MaterialTheme.shapes.extraLarge,
            title = {
                Text(
                    stringResource(R.string.delete_expense_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    stringResource(
                        R.string.delete_expense_message,
                        expense.description,
                        formatInr(expense.amountPaise),
                        state.groupName,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            dismissButton = {
                OutlinedActionButton(stringResource(R.string.action_cancel), onClick = {
                    confirmDelete =
                        false
                })
            },
            confirmButton = {
                DestructiveButton(stringResource(R.string.detail_delete), onClick = {
                    confirmDelete = false
                    onDelete()
                }, filled = true)
            },
        )
    }
}

@Composable
private fun SummaryCard(state: ExpenseDetailUiState, expense: Expense) {
    val colors = SplitEasyTheme.colors
    val payer = state.nameOf(expense.paidByMemberId) ?: stringResource(R.string.you)
    SplitEasyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryIcon(expense.category, size = 48.dp)
                Column {
                    Text(
                        expense.description,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.ink,
                    )
                    Text(
                        stringResource(
                            R.string.detail_category_date,
                            stringResource(expense.category.style().label),
                            formatDayHeader(expense.date),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                    )
                }
            }
            AmountText(
                expense.amountPaise,
                style = MaterialTheme.typography.displaySmall,
                color = colors.ink,
                modifier = Modifier.padding(top = 16.dp),
            )
            val split = when (expense.splitType) {
                SplitType.EQUAL -> stringResource(
                    R.string.detail_paid_split_equal,
                    payer,
                    expense.sharesPaise.size,
                )

                SplitType.EXACT -> stringResource(R.string.detail_paid_split_exact, payer)

                SplitType.PERCENT -> stringResource(R.string.detail_paid_split_percent, payer)
            }
            Text(split, style = MaterialTheme.typography.bodyMedium, color = colors.onNeutralTint)
        }
    }
}
