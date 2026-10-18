package com.split.easy.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.split.easy.BuildConfig
import com.split.easy.R
import com.split.easy.config.FeatureFlags
import com.split.easy.domain.model.ThemeSetting
import com.split.easy.ui.components.CardDivider
import com.split.easy.ui.components.InitialsAvatar
import com.split.easy.ui.components.OutlinedActionButton
import com.split.easy.ui.components.ProfileIconAvatar
import com.split.easy.ui.components.SectionHeader
import com.split.easy.ui.components.SplitEasyCard
import com.split.easy.ui.components.TextActionButton
import com.split.easy.ui.theme.SplitEasyTheme
import com.split.easy.ui.upi.UpiIdSheet
import com.split.easy.ui.upi.UpiIdTarget

@Composable
fun ProfileRoute(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val demoMessage = stringResource(R.string.profile_demo_done)
    LaunchedEffect(viewModel) {
        viewModel.demoLoadedEvents.collect {
            Toast.makeText(context, demoMessage, Toast.LENGTH_SHORT).show()
        }
    }
    ProfileScreen(
        state = state,
        onSaveUpiId = viewModel::onSaveUpiId,
        onThemeChange = viewModel::onThemeChange,
        onLoadDemoData = viewModel::onLoadDemoData,
        showDeveloperOptions = BuildConfig.DEBUG,
    )
}

/** Profile (09), without the P2 rows (Reminders, Export data). Sign out arrives with sign-in in M4. */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onSaveUpiId: (String) -> Unit,
    onThemeChange: (ThemeSetting) -> Unit,
    onLoadDemoData: () -> Unit,
    showDeveloperOptions: Boolean,
) {
    val colors = SplitEasyTheme.colors
    var editUpi by rememberSaveable { mutableStateOf(false) }
    var pickTheme by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
    ) {
        Text(
            stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.ink,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 8.dp)
                .semantics { heading() },
        )

        SplitEasyCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (state.user.isPlaceholder) {
                    ProfileIconAvatar(
                        size = 56.dp,
                    )
                } else {
                    InitialsAvatar(state.user.displayName, size = 56.dp)
                }
                Column {
                    Text(
                        if (state.user.isPlaceholder) {
                            stringResource(
                                R.string.profile_local_name,
                            )
                        } else {
                            state.user.displayName
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.ink,
                    )
                    if (state.user.isPlaceholder) {
                        Text(
                            stringResource(R.string.profile_local_caption),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted,
                        )
                    }
                }
            }
        }

        SectionHeader(stringResource(R.string.profile_payments))
        SplitEasyCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { editUpi = true }
                    .heightIn(min = 64.dp)
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.profile_upi),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.ink,
                    )
                    Text(
                        state.upiId?.let { stringResource(R.string.profile_upi_value, it) }
                            ?: stringResource(R.string.profile_upi_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                    )
                }
                TextActionButton(
                    stringResource(
                        if (state.upiId ==
                            null
                        ) {
                            R.string.profile_add
                        } else {
                            R.string.profile_edit
                        },
                    ),
                    onClick = { editUpi = true },
                )
            }
        }

        if (FeatureFlags.SHOW_SYNC_STATUS) {
            SectionHeader(stringResource(R.string.profile_sync))
            SplitEasyCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(colors.oweTint, MaterialTheme.shapes.small),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_sync),
                            null,
                            tint = colors.onOweTint,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column {
                        Text(
                            pluralStringResource(
                                R.plurals.profile_changes_waiting,
                                state.pendingChanges,
                                state.pendingChanges,
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.ink,
                        )
                        Text(
                            stringResource(R.string.profile_sync_later),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted,
                        )
                    }
                }
            }
        }

        SectionHeader(stringResource(R.string.profile_app))
        SplitEasyCard(Modifier.fillMaxWidth()) {
            SettingRow(
                stringResource(
                    R.string.profile_theme,
                ),
                stringResource(state.theme.label()),
                onClick = {
                    pickTheme =
                        true
                },
            )
        }

        if (showDeveloperOptions) {
            SectionHeader(stringResource(R.string.profile_developer))
            SplitEasyCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onLoadDemoData)
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        stringResource(R.string.profile_demo_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.ink,
                    )
                    Text(
                        stringResource(R.string.profile_demo_data_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                    )
                }
            }
        }

        Text(
            stringResource(R.string.profile_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            textAlign = TextAlign.Center,
        )
    }

    if (editUpi) {
        UpiIdSheet(
            target = UpiIdTarget.Mine,
            current = state.upiId,
            onSave = {
                onSaveUpiId(it)
                editUpi = false
            },
            onDismiss = { editUpi = false },
        )
    }
    if (pickTheme) {
        ThemeDialog(state.theme, onPick = {
            onThemeChange(it)
            pickTheme = false
        }, onDismiss = { pickTheme = false })
    }
}

private fun ThemeSetting.label() = when (this) {
    ThemeSetting.SYSTEM -> R.string.profile_theme_system
    ThemeSetting.LIGHT -> R.string.profile_theme_light
    ThemeSetting.DARK -> R.string.profile_theme_dark
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    val colors = SplitEasyTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
    }
}

/** System default / Light / Dark. */
@Composable
private fun ThemeDialog(
    current: ThemeSetting,
    onPick: (ThemeSetting) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SplitEasyTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                stringResource(R.string.profile_theme),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(Modifier.selectableGroup()) {
                ThemeSetting.entries.forEachIndexed { index, theme ->
                    if (index > 0) CardDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = theme == current,
                                role = Role.RadioButton,
                                onClick = { onPick(theme) },
                            )
                            .heightIn(min = 52.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = theme == current,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = colors.primary),
                        )
                        Text(
                            stringResource(theme.label()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.ink,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            OutlinedActionButton(stringResource(R.string.action_cancel), onClick = onDismiss)
        },
    )
}
