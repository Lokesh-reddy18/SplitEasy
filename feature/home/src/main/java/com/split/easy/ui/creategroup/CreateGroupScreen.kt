package com.split.easy.ui.creategroup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.split.easy.core.designsystem.R
import com.split.easy.ui.components.AvatarTint
import com.split.easy.ui.components.CardDivider
import com.split.easy.ui.components.InitialsAvatar
import com.split.easy.ui.components.OutlinedActionButton
import com.split.easy.ui.components.PrimaryButton
import com.split.easy.ui.components.SplitEasyCard
import com.split.easy.ui.components.SplitEasyTopBar
import com.split.easy.ui.components.TopBarNavigation
import com.split.easy.ui.components.avatarTintFor
import com.split.easy.ui.theme.SplitEasyTheme

@Composable
fun CreateGroupRoute(
    onClose: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.createdGroupId.collect { onCreated(it) }
    }
    CreateGroupScreen(
        state = state,
        onClose = onClose,
        onNameChange = viewModel::onNameChange,
        onGuestInputChange = viewModel::onGuestInputChange,
        onAddGuest = viewModel::onAddGuest,
        onRemoveGuest = viewModel::onRemoveGuest,
        onCreate = viewModel::onCreate,
    )
}

/** New group (11): a name, you as creator, and guests added by name. */
@Composable
fun CreateGroupScreen(
    state: CreateGroupUiState,
    onClose: () -> Unit,
    onNameChange: (String) -> Unit,
    onGuestInputChange: (String) -> Unit,
    onAddGuest: () -> Unit,
    onRemoveGuest: (String) -> Unit,
    onCreate: () -> Unit,
) {
    val colors = SplitEasyTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surface)
            .imePadding(),
    ) {
        SplitEasyTopBar(
            title = stringResource(R.string.create_group_title),
            navigation = TopBarNavigation.CLOSE,
            onNavigate = onClose,
            actions = {
                PrimaryButton(
                    text = stringResource(R.string.create_group_create),
                    onClick = onCreate,
                    enabled = state.canCreate,
                )
            },
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column {
                FieldLabel(stringResource(R.string.create_group_name_label))
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    placeholder = { Text(stringResource(R.string.create_group_name_hint)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = MaterialTheme.shapes.small,
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
            Column {
                FieldLabel(stringResource(R.string.create_group_members))
                Text(
                    stringResource(R.string.create_group_members_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
                MembersCard(state, onGuestInputChange, onAddGuest, onRemoveGuest)
            }
        }
        Text(
            pluralStringResource(
                R.plurals.create_group_people,
                state.peopleCount,
                state.peopleCount,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun MembersCard(
    state: CreateGroupUiState,
    onGuestInputChange: (String) -> Unit,
    onAddGuest: () -> Unit,
    onRemoveGuest: (String) -> Unit,
) {
    val colors = SplitEasyTheme.colors
    SplitEasyCard(Modifier.fillMaxWidth()) {
        // Shown as "You" (design 11), with initials from your Google name.
        PersonRow(
            name = stringResource(R.string.you),
            caption = stringResource(R.string.create_group_creator),
            tint = AvatarTint.PRIMARY,
            avatarName = state.creatorName,
        )
        state.guests.forEach { guest ->
            CardDivider()
            PersonRow(
                name = guest,
                caption = stringResource(R.string.create_group_guest),
                tint = avatarTintFor(guest),
            ) {
                IconButton(onClick = { onRemoveGuest(guest) }) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(
                            R.string.create_group_remove_guest,
                            guest,
                        ),
                        tint = colors.muted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        CardDivider()
        Row(
            Modifier.padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.guestInput,
                onValueChange = onGuestInputChange,
                placeholder = { Text(stringResource(R.string.create_group_guest_hint)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                isError = state.duplicateGuest != null,
                supportingText = state.duplicateGuest?.let {
                    { Text(stringResource(R.string.create_group_duplicate, it)) }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = colors.surface,
                    focusedBorderColor = colors.surface,
                    errorBorderColor = colors.surface,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onAddGuest() }),
                modifier = Modifier.weight(1f),
            )
            OutlinedActionButton(
                text = stringResource(R.string.create_group_add),
                onClick = onAddGuest,
                enabled = state.canAddGuest,
                contentColor = colors.onPrimaryTint,
            )
        }
    }
}

@Composable
private fun PersonRow(
    name: String,
    caption: String,
    tint: AvatarTint,
    /** Name used for the initials, when it differs from [name] ("You" → "Ananya Rao"). */
    avatarName: String = name,
    trailing: @Composable () -> Unit = {},
) {
    val colors = SplitEasyTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialsAvatar(avatarName, size = 36.dp, tint = tint)
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
            Text(caption, style = MaterialTheme.typography.labelSmall, color = colors.muted)
        }
        trailing()
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = SplitEasyTheme.colors.muted,
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = SplitEasyTheme.colors.border,
    focusedBorderColor = SplitEasyTheme.colors.primary,
)
