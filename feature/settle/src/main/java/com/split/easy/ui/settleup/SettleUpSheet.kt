package com.split.easy.ui.settleup

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.split.easy.core.designsystem.R
import com.split.easy.domain.money.formatInr
import com.split.easy.domain.settlement.AmountCheck
import com.split.easy.ui.components.IndianGroupingTransformation
import com.split.easy.ui.components.InitialsAvatar
import com.split.easy.ui.components.PrimaryButton
import com.split.easy.ui.components.TextActionButton
import com.split.easy.ui.theme.SplitEasyTheme
import com.split.easy.ui.theme.TABULAR_NUMBERS
import com.split.easy.ui.upi.UpiIdSheet
import com.split.easy.ui.upi.UpiIdTarget

/** Settle up (06, 21): shown as a sheet over the previous screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpRoute(onClose: () -> Unit, viewModel: SettleUpViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editGuestUpi by rememberSaveable { mutableStateOf(false) }
    val chooserTitle = stringResource(R.string.settle_chooser)

    LaunchedEffect(viewModel) { viewModel.doneEvents.collect { onClose() } }

    // UPI apps return "response" in the result intent; a cancelled or empty result means no response.
    val upiLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            viewModel.onUpiResult(result.data?.getStringExtra("response"))
        }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SplitEasyTheme.colors.surface,
    ) {
        SettleUpContent(
            state = state,
            onAmountChange = viewModel::onAmountChange,
            onNoteChange = viewModel::onNoteChange,
            onPay = {
                val uri = viewModel.upiUri() ?: return@SettleUpContent
                val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
                try {
                    upiLauncher.launch(Intent.createChooser(intent, chooserTitle))
                } catch (_: ActivityNotFoundException) {
                    viewModel.onNoUpiApp()
                }
            },
            onRecordCash = viewModel::onRecordCash,
            onAddGuestUpi = { editGuestUpi = true },
        )
    }

    val payee = state.payee
    if (editGuestUpi && payee != null) {
        UpiIdSheet(
            target = UpiIdTarget.Guest(payee.id, payee.displayName, state.groupName),
            current = payee.upiId,
            onSave = {
                viewModel.onSetGuestUpiId(it)
                editGuestUpi = false
            },
            onDismiss = { editGuestUpi = false },
        )
    }
}

@Composable
fun SettleUpContent(
    state: SettleUpUiState,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onPay: () -> Unit,
    onRecordCash: () -> Unit,
    onAddGuestUpi: () -> Unit,
) {
    val colors = SplitEasyTheme.colors
    val payee = state.payee ?: return
    val name = payee.displayName
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = 16.dp),
    ) {
        Text(
            stringResource(R.string.settle_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
        )
        Text(state.groupName, style = MaterialTheme.typography.bodyMedium, color = colors.muted)

        // Payee card
        Row(
            Modifier
                .padding(top = 18.dp)
                .fillMaxWidth()
                .background(colors.background, MaterialTheme.shapes.medium)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InitialsAvatar(name, size = 48.dp)
            Column {
                Text(
                    stringResource(R.string.settle_you_pay),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
                Text(name, style = MaterialTheme.typography.titleSmall, color = colors.ink)
                val upiId = payee.upiId
                if (upiId != null) {
                    Text(
                        upiId,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onNeutralTint,
                    )
                } else {
                    Text(
                        stringResource(R.string.settle_no_upi_yet),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.owe,
                    )
                }
            }
        }

        if (state.payablePaise <= 0 && !state.isLoading) {
            Text(
                stringResource(R.string.settle_nothing_to_pay, name),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
                modifier = Modifier.padding(vertical = 24.dp),
            )
            return@Column
        }

        // Amount
        Text(
            stringResource(R.string.editor_amount),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.muted,
            modifier = Modifier.padding(top = 18.dp),
        )
        val check = state.amountCheck
        TextField(
            value = state.amountText,
            onValueChange = onAmountChange,
            prefix = {
                Text("₹ ", style = MaterialTheme.typography.headlineMedium, color = colors.muted)
            },
            textStyle = MaterialTheme.typography.displaySmall.copy(
                color = colors.ink,
                fontFeatureSettings = TABULAR_NUMBERS,
            ),
            singleLine = true,
            isError = check != null && check != AmountCheck.OK,
            visualTransformation = IndianGroupingTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedIndicatorColor = colors.primary,
                unfocusedIndicatorColor = colors.primary,
                errorIndicatorColor = colors.owe,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        val amountHelp = when (check) {
            AmountCheck.ABOVE_PAYABLE, AmountCheck.ABOVE_LIMIT ->
                stringResource(R.string.settle_above, name, formatInr(state.payablePaise))

            else -> if (state.amountPaise == state.payablePaise) {
                stringResource(R.string.settle_full_balance)
            } else {
                stringResource(R.string.settle_part_of, name, formatInr(state.payablePaise))
            }
        }
        Text(
            amountHelp,
            style = MaterialTheme.typography.bodySmall,
            color = if (check == AmountCheck.ABOVE_PAYABLE ||
                check == AmountCheck.ABOVE_LIMIT
            ) {
                colors.owe
            } else {
                colors.muted
            },
            modifier = Modifier.padding(top = 6.dp),
        )

        // Note
        OutlinedTextField(
            value = state.note,
            onValueChange = onNoteChange,
            label = { Text(stringResource(R.string.settle_note)) },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = colors.border,
                focusedBorderColor = colors.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )

        // Pay with UPI
        val payLabel = state.amountPaise?.takeIf { check == AmountCheck.OK }?.let {
            stringResource(R.string.settle_pay_with_upi, formatInr(it))
        } ?: stringResource(R.string.settle_pay_with_upi_plain)
        PrimaryButton(
            text = payLabel,
            onClick = onPay,
            enabled = state.canPayWithUpi,
            icon = R.drawable.ic_upi_card,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp),
        )
        val help = when {
            state.outcome == PayOutcome.NO_UPI_APP -> stringResource(R.string.settle_no_upi_app)

            state.outcome == PayOutcome.NOT_COMPLETED -> stringResource(R.string.settle_failed)

            payee.upiId == null && payee.isGuest -> stringResource(
                R.string.settle_no_upi_guest,
                name,
            )

            payee.upiId == null -> stringResource(R.string.settle_no_upi_user, name)

            payee.isGuest -> stringResource(R.string.settle_upi_help_guest, name)

            else -> stringResource(R.string.settle_upi_help, name)
        }
        Text(
            help,
            style = MaterialTheme.typography.bodySmall,
            color = if (state.outcome == PayOutcome.NONE) colors.muted else colors.owe,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
        if (payee.upiId == null && payee.isGuest) {
            TextActionButton(
                stringResource(R.string.settle_add_guest_upi, name),
                onClick = onAddGuestUpi,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        TextActionButton(
            stringResource(
                if (state.outcome ==
                    PayOutcome.NOT_COMPLETED
                ) {
                    R.string.settle_record_after_failure
                } else {
                    R.string.settle_record_cash
                },
            ),
            onClick = onRecordCash,
            enabled = state.canRecord,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
