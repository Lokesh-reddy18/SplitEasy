package com.split.easy.ui.upi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.split.easy.core.designsystem.R
import com.split.easy.domain.upi.UpiPayment
import com.split.easy.ui.components.PrimaryButton
import com.split.easy.ui.theme.SplitEasyTheme

/** Whose UPI ID is being edited. */
sealed interface UpiIdTarget {
    /** Your own (16): saved on your member row in every group (F2). */
    data object Mine : UpiIdTarget

    /** A guest's (17): anyone can set it, and the change is logged in the feed (F1, H5). */
    data class Guest(val memberId: String, val name: String, val groupName: String) : UpiIdTarget
}

/** The UPI ID bottom sheet, designs 16 (yours) and 17 (a guest's). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiIdSheet(
    target: UpiIdTarget,
    current: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SplitEasyTheme.colors.surface,
    ) {
        UpiIdContent(target, current, onSave)
    }
}

@Composable
fun UpiIdContent(target: UpiIdTarget, current: String?, onSave: (String) -> Unit) {
    val colors = SplitEasyTheme.colors
    var text by rememberSaveable { mutableStateOf(current.orEmpty()) }
    var showError by rememberSaveable { mutableStateOf(false) }
    val trimmed = text.trim()
    val valid = UpiPayment.isValidUpiId(trimmed)

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = 16.dp),
    ) {
        val (title, help) = when (target) {
            UpiIdTarget.Mine -> stringResource(R.string.upi_my_title) to
                stringResource(R.string.upi_my_help)

            is UpiIdTarget.Guest ->
                stringResource(R.string.upi_guest_title, target.name) to
                    stringResource(R.string.upi_guest_help, target.name)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.ink)
        Text(
            help,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            modifier = Modifier.padding(top = 4.dp),
        )
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it.trim()
                showError = false
            },
            label = { Text(stringResource(R.string.upi_label)) },
            singleLine = true,
            isError = showError,
            supportingText = {
                Text(
                    stringResource(
                        if (showError) R.string.upi_invalid else R.string.upi_format_help,
                    ),
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = colors.border,
                focusedBorderColor = colors.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
        )
        if (target is UpiIdTarget.Guest && valid && trimmed != current) {
            val warning = if (current == null) {
                stringResource(
                    R.string.upi_guest_warning_set,
                    target.groupName,
                    target.name,
                    trimmed,
                )
            } else {
                stringResource(
                    R.string.upi_guest_warning_changed,
                    target.groupName,
                    target.name,
                    current,
                    trimmed,
                )
            }
            Row(
                Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .background(colors.oweTint, MaterialTheme.shapes.small)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_info),
                    null,
                    tint = colors.onOweTint,
                    modifier = Modifier.size(18.dp),
                )
                Text(warning, style = MaterialTheme.typography.bodySmall, color = colors.onOweTint)
            }
        }
        PrimaryButton(
            text = stringResource(R.string.upi_save),
            onClick = { if (valid) onSave(trimmed) else showError = true },
            enabled = trimmed.isNotEmpty() && trimmed != current,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
        )
    }
}
