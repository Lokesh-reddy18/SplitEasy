package com.split.easy.ui.groupsettings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.split.easy.R
import com.split.easy.domain.group.GroupCheck
import com.split.easy.domain.money.formatInr
import com.split.easy.ui.components.CardDivider
import com.split.easy.ui.components.ChipStyle
import com.split.easy.ui.components.DestructiveButton
import com.split.easy.ui.components.InitialsAvatar
import com.split.easy.ui.components.OutlinedActionButton
import com.split.easy.ui.components.PrimaryButton
import com.split.easy.ui.components.ProfileIconAvatar
import com.split.easy.ui.components.SectionHeader
import com.split.easy.ui.components.SplitEasyCard
import com.split.easy.ui.components.SplitEasyTopBar
import com.split.easy.ui.components.StatusChip
import com.split.easy.ui.components.TextActionButton
import com.split.easy.ui.theme.SplitEasyTheme
import com.split.easy.ui.upi.UpiIdSheet
import com.split.easy.ui.upi.UpiIdTarget

@Composable
fun GroupSettingsRoute(
    onBack: () -> Unit,
    onLeft: () -> Unit,
    viewModel: GroupSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.leftEvents.collect { onLeft() } }
    GroupSettingsScreen(
        state = state,
        actions = SettingsActions(
            onBack = onBack,
            onRename = viewModel::onRename,
            onAddGuest = viewModel::onAddGuest,
            onSetGuestUpiId = viewModel::onSetGuestUpiId,
            onRemove = viewModel::onRemove,
            onArchive = viewModel::onArchive,
            onUnarchive = viewModel::onUnarchive,
            onLeave = viewModel::onLeave,
        ),
    )
}

data class SettingsActions(
    val onBack: () -> Unit = {},
    val onRename: (String) -> Unit = {},
    val onAddGuest: (String) -> Unit = {},
    val onSetGuestUpiId: (memberId: String, upiId: String) -> Unit = { _, _ -> },
    val onRemove: (String) -> Unit = {},
    val onArchive: () -> Unit = {},
    val onUnarchive: () -> Unit = {},
    val onLeave: () -> Unit = {},
)

private sealed interface Dialog {
    data object Rename : Dialog

    data object AddGuest : Dialog

    data object Archive : Dialog

    data object Leave : Dialog

    data class Remove(val memberId: String, val name: String) : Dialog

    data class GuestUpi(val memberId: String) : Dialog
}

/** Group settings (12). Disabled actions say why ("Settle ₹X first"). */
@Composable
fun GroupSettingsScreen(state: GroupSettingsUiState, actions: SettingsActions) {
    val colors = SplitEasyTheme.colors
    val group = state.group
    var dialog by rememberSaveable(stateSaver = DialogSaver) { mutableStateOf<Dialog?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        SplitEasyTopBar(
            title = stringResource(R.string.settings_title),
            subtitle = group?.name,
            onNavigate = actions.onBack,
        )
        if (group == null || state.me == null) return@Column

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            SectionHeader(stringResource(R.string.settings_name))
            SplitEasyCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .heightIn(min = 52.dp)
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        group.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.canEditGuests) {
                        TextActionButton(stringResource(R.string.settings_rename), onClick = {
                            dialog =
                                Dialog.Rename
                        })
                    }
                }
            }

            SectionHeader(stringResource(R.string.settings_members, state.members.size))
            SplitEasyCard(Modifier.fillMaxWidth()) {
                state.members.forEachIndexed { index, row ->
                    if (index > 0) CardDivider()
                    MemberRowView(
                        row = row,
                        viewerIsCreator = state.isCreator,
                        canEditGuests = state.canEditGuests,
                        onEditUpi = { dialog = Dialog.GuestUpi(row.member.id) },
                        onRemove = {
                            dialog = Dialog.Remove(row.member.id, row.member.displayName)
                        },
                    )
                }
                if (state.canEditGuests) {
                    CardDivider()
                    TextActionButton(
                        stringResource(R.string.settings_add_guest),
                        onClick = { dialog = Dialog.AddGuest },
                        icon = R.drawable.ic_add,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            SectionHeader(stringResource(R.string.settings_group))
            SplitEasyCard(Modifier.fillMaxWidth()) {
                when {
                    group.isArchived && state.isCreator -> ActionRow(
                        stringResource(R.string.settings_unarchive),
                        check = state.unarchiveCheck,
                        icon = R.drawable.ic_archive,
                        destructive = false,
                        onClick = actions.onUnarchive,
                    )

                    state.isCreator -> ActionRow(
                        stringResource(R.string.settings_archive),
                        check = state.archiveCheck,
                        icon = R.drawable.ic_archive,
                        destructive = false,
                        onClick = { dialog = Dialog.Archive },
                    )

                    else -> ActionRow(
                        stringResource(R.string.settings_leave),
                        check = state.leaveCheck,
                        icon = null,
                        destructive = true,
                        onClick = { dialog = Dialog.Leave },
                    )
                }
            }
            if (state.isCreator && !group.isArchived) {
                Text(
                    stringResource(R.string.settings_creator_note),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.muted,
                    modifier = Modifier.padding(start = 4.dp, top = 10.dp),
                )
            }
        }
    }

    when (val d = dialog) {
        Dialog.Rename -> TextDialog(
            title = stringResource(R.string.settings_rename_title),
            label = stringResource(R.string.settings_name),
            initial = group?.name.orEmpty(),
            confirm = stringResource(R.string.settings_save),
            capitalization = KeyboardCapitalization.Sentences,
            onConfirm = {
                actions.onRename(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        Dialog.AddGuest -> TextDialog(
            title = stringResource(R.string.settings_add_guest_title),
            label = stringResource(R.string.settings_guest_name),
            initial = "",
            confirm = stringResource(R.string.settings_add),
            capitalization = KeyboardCapitalization.Words,
            onConfirm = {
                actions.onAddGuest(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        Dialog.Archive -> ConfirmDialog(
            title = stringResource(R.string.settings_archive_confirm_title, group?.name.orEmpty()),
            body = stringResource(R.string.settings_archive_confirm_body),
            confirm = stringResource(R.string.settings_archive),
            onConfirm = {
                actions.onArchive()
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        Dialog.Leave -> ConfirmDialog(
            title = stringResource(R.string.settings_leave_confirm_title, group?.name.orEmpty()),
            body = stringResource(R.string.settings_leave_confirm_body),
            confirm = stringResource(R.string.settings_leave),
            onConfirm = {
                actions.onLeave()
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        is Dialog.Remove -> ConfirmDialog(
            title = stringResource(R.string.settings_remove_confirm_title, d.name),
            body = stringResource(R.string.settings_remove_confirm_body, d.name),
            confirm = stringResource(R.string.settings_remove),
            onConfirm = {
                actions.onRemove(d.memberId)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        is Dialog.GuestUpi -> {
            val guest = state.members.firstOrNull { it.member.id == d.memberId }?.member
            if (guest != null) {
                UpiIdSheet(
                    target = UpiIdTarget.Guest(guest.id, guest.displayName, group?.name.orEmpty()),
                    current = guest.upiId,
                    onSave = {
                        actions.onSetGuestUpiId(guest.id, it)
                        dialog = null
                    },
                    onDismiss = { dialog = null },
                )
            }
        }

        null -> Unit
    }
}

@Composable
private fun MemberRowView(
    row: MemberRow,
    viewerIsCreator: Boolean,
    canEditGuests: Boolean,
    onEditUpi: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = SplitEasyTheme.colors
    val member = row.member
    val you = stringResource(R.string.you)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (row.isMe &&
            member.displayName == you
        ) {
            ProfileIconAvatar(size = 36.dp)
        } else {
            InitialsAvatar(member.displayName, size = 36.dp)
        }
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    if (row.isMe) you else member.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.ink,
                )
                if (member.isGuest) {
                    StatusChip(
                        stringResource(R.string.settings_guest),
                        ChipStyle.NEUTRAL,
                    )
                }
            }
            val upi = member.upiId
            val caption = when {
                row.isCreator && upi != null -> stringResource(R.string.settings_creator_upi, upi)
                row.isCreator -> stringResource(R.string.settings_creator)
                upi != null -> upi
                member.isGuest -> stringResource(R.string.settings_no_upi)
                else -> null
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (caption !=
                    null
                ) {
                    Text(
                        caption,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                }
                if (member.isGuest && canEditGuests) {
                    Text(
                        " · " +
                            stringResource(
                                if (upi ==
                                    null
                                ) {
                                    R.string.settings_add_upi
                                } else {
                                    R.string.settings_edit_upi
                                },
                            ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText,
                        modifier = Modifier
                            .clickable(onClick = onEditUpi)
                            .padding(vertical = 12.dp),
                    )
                }
            }
        }
        if (viewerIsCreator && !row.isMe && !row.isCreator) {
            when (val check = row.removeCheck) {
                GroupCheck.Allowed -> TextActionButton(
                    stringResource(R.string.settings_remove),
                    onClick = onRemove,
                    color = colors.owe,
                )

                else -> blockedReason(check)?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = colors.muted)
                }
            }
        }
    }
}

/** "Settle ₹300 first" or "Resolve pending payments first". */
@Composable
private fun blockedReason(check: GroupCheck): String? = when (check) {
    is GroupCheck.SettleFirst -> stringResource(
        R.string.settings_settle_first,
        formatInr(check.outstandingPaise),
    )

    GroupCheck.ResolvePendingFirst -> stringResource(R.string.settings_resolve_pending)

    else -> null
}

@Composable
private fun ActionRow(
    text: String,
    check: GroupCheck,
    icon: Int?,
    destructive: Boolean,
    onClick: () -> Unit,
) {
    val colors = SplitEasyTheme.colors
    val enabled = check == GroupCheck.Allowed
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    !enabled -> colors.muted
                    destructive -> colors.owe
                    else -> colors.ink
                },
            )
            blockedReason(check)?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = colors.muted)
            }
        }
        if (icon != null) {
            Icon(
                painterResource(icon),
                null,
                tint = if (enabled) colors.ink else colors.border,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TextDialog(
    title: String,
    label: String,
    initial: String,
    confirm: String,
    capitalization: KeyboardCapitalization,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SplitEasyTheme.colors
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(40) },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = capitalization),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = colors.border,
                    focusedBorderColor = colors.primary,
                ),
            )
        },
        dismissButton = {
            OutlinedActionButton(stringResource(R.string.action_cancel), onClick = onDismiss)
        },
        confirmButton = {
            PrimaryButton(
                confirm,
                onClick = { onConfirm(text.trim()) },
                enabled =
                    text.isNotBlank() && text.trim() != initial,
            )
        },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SplitEasyTheme.colors.surface,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        dismissButton = {
            OutlinedActionButton(stringResource(R.string.action_cancel), onClick = onDismiss)
        },
        confirmButton = { DestructiveButton(confirm, onClick = onConfirm, filled = true) },
    )
}

/** Lets the open dialog survive rotation. */
private val DialogSaver = Saver<Dialog?, String>(
    save = { d ->
        when (d) {
            Dialog.Rename -> "rename"
            Dialog.AddGuest -> "guest"
            Dialog.Archive -> "archive"
            Dialog.Leave -> "leave"
            is Dialog.Remove -> "remove|${d.memberId}|${d.name}"
            is Dialog.GuestUpi -> "upi|${d.memberId}"
            null -> ""
        }
    },
    restore = { s ->
        val parts = s.split('|')
        when (parts[0]) {
            "rename" -> Dialog.Rename
            "guest" -> Dialog.AddGuest
            "archive" -> Dialog.Archive
            "leave" -> Dialog.Leave
            "remove" -> Dialog.Remove(parts[1], parts[2])
            "upi" -> Dialog.GuestUpi(parts[1])
            else -> null
        }
    },
)
