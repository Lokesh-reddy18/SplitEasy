package com.split.easy.ui.expense.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.split.easy.core.designsystem.R
import com.split.easy.domain.model.Category
import com.split.easy.domain.model.Member
import com.split.easy.ui.components.CategoryIcon
import com.split.easy.ui.components.InitialsAvatar
import com.split.easy.ui.components.style
import com.split.easy.ui.theme.SplitEasyTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheet(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SplitEasyTheme.colors.surface,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = SplitEasyTheme.colors.ink,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            content()
        }
    }
}

@Composable
private fun PickerRow(
    selected: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    label: String,
) {
    val colors = SplitEasyTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        leading()
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = colors.primaryText,
            )
        } else {
            Spacer(Modifier.size(24.dp))
        }
    }
}

/** "Paid by": anyone in the group (current members, plus anyone already on this expense). */
@Composable
fun PaidByPicker(
    members: List<Member>,
    myMemberId: String?,
    selectedId: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    PickerSheet(stringResource(R.string.editor_paid_by), onDismiss) {
        members.forEach { member ->
            val name = if (member.id ==
                myMemberId
            ) {
                stringResource(R.string.you)
            } else {
                member.displayName
            }
            PickerRow(
                selected = member.id == selectedId,
                onClick = { onPick(member.id) },
                leading = { InitialsAvatar(member.displayName, size = 36.dp) },
                label = name,
            )
        }
    }
}

/** The fixed category list, one icon each (Q12). */
@Composable
fun CategoryPicker(selected: Category, onPick: (Category) -> Unit, onDismiss: () -> Unit) {
    PickerSheet(stringResource(R.string.editor_category), onDismiss) {
        Category.entries.forEach { category ->
            PickerRow(
                selected = category == selected,
                onClick = { onPick(category) },
                leading = { CategoryIcon(category, size = 36.dp) },
                label = stringResource(category.style().label),
            )
        }
    }
}

/** Any past date, or up to 30 days ahead (section E). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDatePicker(
    selected: LocalDate,
    maxDate: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val maxMillis = maxDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selected.atStartOfDay(
            ZoneOffset.UTC,
        ).toInstant().toEpochMilli(),
        yearRange = 2000..maxDate.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= maxMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}
