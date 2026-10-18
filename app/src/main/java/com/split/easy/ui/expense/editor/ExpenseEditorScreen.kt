package com.split.easy.ui.expense.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.split.easy.R
import com.split.easy.domain.model.Category
import com.split.easy.domain.model.SplitType
import com.split.easy.domain.money.formatInr
import com.split.easy.domain.split.formatBasisPoints
import com.split.easy.ui.components.AmountText
import com.split.easy.ui.components.CardDivider
import com.split.easy.ui.components.DestructiveButton
import com.split.easy.ui.components.OutlinedActionButton
import com.split.easy.ui.components.PrimaryButton
import com.split.easy.ui.components.SplitEasyCard
import com.split.easy.ui.components.SplitEasyTopBar
import com.split.easy.ui.components.TopBarNavigation
import com.split.easy.ui.components.style
import com.split.easy.ui.format.formatShortDate
import com.split.easy.ui.theme.SplitEasyTheme
import com.split.easy.ui.theme.TABULAR_NUMBERS
import java.time.LocalDate

@Composable
fun ExpenseEditorRoute(onClose: () -> Unit, viewModel: ExpenseEditorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.savedEvents.collect { onClose() } }
    ExpenseEditorScreen(
        state = state,
        actions = EditorActions(
            onClose = onClose,
            onSave = viewModel::onSave,
            onAmountChange = viewModel::onAmountChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onPaidByChange = viewModel::onPaidByChange,
            onCategoryChange = viewModel::onCategoryChange,
            onDateChange = viewModel::onDateChange,
            onSplitTypeChange = viewModel::onSplitTypeChange,
            onToggleIncluded = viewModel::onToggleIncluded,
            onExactChange = viewModel::onExactChange,
            onPercentChange = viewModel::onPercentChange,
        ),
    )
}

/** Everything the editor can do, grouped so the screen's signature stays readable. */
data class EditorActions(
    val onClose: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onAmountChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onPaidByChange: (String) -> Unit = {},
    val onCategoryChange: (Category) -> Unit = {},
    val onDateChange: (LocalDate) -> Unit = {},
    val onSplitTypeChange: (SplitType) -> Unit = {},
    val onToggleIncluded: (String) -> Unit = {},
    val onExactChange: (String, String) -> Unit = { _, _ -> },
    val onPercentChange: (String, String) -> Unit = { _, _ -> },
)

private enum class Picker { PAID_BY, CATEGORY, DATE }

/** Add or edit an expense (04). */
@Composable
fun ExpenseEditorScreen(state: ExpenseEditorUiState, actions: EditorActions) {
    val colors = SplitEasyTheme.colors
    val form = state.form
    val result = state.result
    var openPicker by rememberSaveable { mutableStateOf<Picker?>(null) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val close = { if (state.hasChanges) confirmDiscard = true else actions.onClose() }
    BackHandler(onBack = close)

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surface)
            .imePadding(),
    ) {
        SplitEasyTopBar(
            title = stringResource(
                if (state.isEditing) R.string.editor_edit_title else R.string.editor_add_title,
            ),
            subtitle = state.groupName,
            navigation = TopBarNavigation.CLOSE,
            onNavigate = close,
            actions = {
                PrimaryButton(
                    stringResource(R.string.editor_save),
                    onClick = actions.onSave,
                    enabled =
                        !state.isSaving && form != null,
                )
            },
        )
        if (form == null || result == null) return@Column

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AmountField(
                text = form.amountText,
                error = result.amountError?.takeIf { state.showErrors || it.isTypingError() },
                autoFocus = !state.isEditing,
                onChange = actions.onAmountChange,
            )
            Column {
                Label(stringResource(R.string.editor_description))
                OutlinedTextField(
                    value = form.description,
                    onValueChange = actions.onDescriptionChange,
                    placeholder = { Text(stringResource(R.string.editor_description_hint)) },
                    singleLine = true,
                    isError = state.showErrors && result.isDescriptionMissing,
                    supportingText = if (state.showErrors && result.isDescriptionMissing) {
                        { Text(stringResource(R.string.editor_error_description)) }
                    } else {
                        null
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = colors.border,
                        focusedBorderColor = colors.primary,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val payer = state.members.firstOrNull { it.id == form.paidByMemberId }
                Tile(
                    label = stringResource(R.string.editor_paid_by),
                    value = if (payer?.id ==
                        state.myMemberId
                    ) {
                        stringResource(R.string.you)
                    } else {
                        payer?.displayName.orEmpty()
                    },
                    onClick = { openPicker = Picker.PAID_BY },
                    modifier = Modifier.weight(1f),
                )
                Tile(
                    label = stringResource(R.string.editor_category),
                    value = stringResource(form.category.style().label),
                    onClick = { openPicker = Picker.CATEGORY },
                    modifier = Modifier.weight(1f),
                )
                Tile(
                    label = stringResource(R.string.editor_date),
                    value = formatShortDate(form.date, state.today),
                    onClick = { openPicker = Picker.DATE },
                    modifier = Modifier.weight(1f),
                )
            }
            if (state.showErrors && result.isDateTooFarAhead) {
                Text(
                    stringResource(R.string.editor_error_date),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.owe,
                )
            }
            Column {
                Label(stringResource(R.string.editor_split))
                SplitTypeSelector(
                    form.splitType,
                    actions.onSplitTypeChange,
                    Modifier.padding(top = 6.dp),
                )
            }
            ParticipantsCard(state, form, result, actions)
        }
        SplitFooter(result.splitStatus)
    }

    when (openPicker) {
        Picker.PAID_BY -> PaidByPicker(
            members = state.members,
            myMemberId = state.myMemberId,
            selectedId = form?.paidByMemberId.orEmpty(),
            onPick = {
                actions.onPaidByChange(it)
                openPicker = null
            },
            onDismiss = { openPicker = null },
        )

        Picker.CATEGORY -> form?.let {
            CategoryPicker(it.category, onPick = { category ->
                actions.onCategoryChange(category)
                openPicker = null
            }, onDismiss = { openPicker = null })
        }

        Picker.DATE -> form?.let {
            ExpenseDatePicker(it.date, state.maxDate, onPick = actions.onDateChange, onDismiss = {
                openPicker =
                    null
            })
        }

        null -> Unit
    }
    if (confirmDiscard) {
        val discardTitle = when {
            state.isEditing -> R.string.editor_discard_edit
            else -> R.string.editor_discard_new
        }
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            containerColor = colors.surface,
            shape = MaterialTheme.shapes.extraLarge,
            title = {
                Text(
                    stringResource(discardTitle),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            dismissButton = {
                OutlinedActionButton(stringResource(R.string.editor_keep_editing), onClick = {
                    confirmDiscard =
                        false
                })
            },
            confirmButton = {
                DestructiveButton(stringResource(R.string.editor_discard), onClick = {
                    confirmDiscard = false
                    actions.onClose()
                }, filled = true)
            },
        )
    }
}

/** Typing mistakes show at once; "missing" ones wait until Save is tapped. */
private fun AmountError.isTypingError() =
    this == AmountError.INVALID || this == AmountError.TOO_MANY_DECIMALS ||
        this == AmountError.ABOVE_LIMIT

/** The big "₹ 2,400" field with a violet underline (04). Opens with the number keypad (M2-Q8). */
@Composable
private fun AmountField(
    text: String,
    error: AmountError?,
    autoFocus: Boolean,
    onChange: (String) -> Unit,
) {
    val colors = SplitEasyTheme.colors
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    if (autoFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    Column {
        Label(stringResource(R.string.editor_amount))
        TextField(
            value = text,
            onValueChange = onChange,
            prefix = {
                Text("₹ ", style = MaterialTheme.typography.headlineMedium, color = colors.muted)
            },
            placeholder = {
                Text("0", style = MaterialTheme.typography.displaySmall, color = colors.border)
            },
            textStyle = MaterialTheme.typography.displaySmall.copy(
                color = colors.ink,
                fontFeatureSettings = TABULAR_NUMBERS,
            ),
            singleLine = true,
            isError = error != null,
            visualTransformation = IndianGroupingTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedIndicatorColor = colors.primary,
                unfocusedIndicatorColor = colors.primary,
                errorIndicatorColor = colors.owe,
            ),
            supportingText = error?.let { { Text(stringResource(it.message())) } },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
    }
}

private fun AmountError.message() = when (this) {
    AmountError.MISSING -> R.string.editor_error_amount_missing
    AmountError.INVALID -> R.string.editor_error_amount_invalid
    AmountError.TOO_MANY_DECIMALS -> R.string.editor_error_amount_decimals
    AmountError.NOT_POSITIVE -> R.string.editor_error_amount_zero
    AmountError.ABOVE_LIMIT -> R.string.editor_error_amount_limit
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = SplitEasyTheme.colors.muted,
    )
}

/** "Paid by / Priya", "Category / Food", "Date / 10 Oct". */
@Composable
private fun Tile(label: String, value: String, onClick: () -> Unit, modifier: Modifier) {
    val colors = SplitEasyTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.small,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.muted)
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = colors.ink,
                maxLines = 1,
            )
        }
    }
}

/** The Equally / Exact / Percent segmented control. */
@Composable
private fun SplitTypeSelector(
    selected: SplitType,
    onSelect: (SplitType) -> Unit,
    modifier: Modifier,
) {
    val colors = SplitEasyTheme.colors
    val options = listOf(
        SplitType.EQUAL to R.string.editor_split_equal,
        SplitType.EXACT to R.string.editor_split_exact,
        SplitType.PERCENT to R.string.editor_split_percent,
    )
    Row(
        modifier
            .fillMaxWidth()
            .background(colors.neutralTint, MaterialTheme.shapes.small)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (type, label) ->
            val isSelected = type == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = {
                        onSelect(type)
                    }),
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) colors.surface else Color.Transparent,
                shadowElevation = if (isSelected) 1.dp else 0.dp,
            ) {
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) colors.ink else colors.onNeutralTint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ParticipantsCard(
    state: ExpenseEditorUiState,
    form: ExpenseForm,
    result: FormResult,
    actions: EditorActions,
) {
    val you = stringResource(R.string.you)
    SplitEasyCard(Modifier.fillMaxWidth()) {
        state.members.forEachIndexed { index, member ->
            if (index > 0) CardDivider()
            val name = if (member.id == state.myMemberId) you else member.displayName
            when (form.splitType) {
                SplitType.EQUAL -> EqualRow(
                    name = name,
                    included = member.id in form.includedMemberIds,
                    sharePaise = result.sharesPaise[member.id],
                    onToggle = { actions.onToggleIncluded(member.id) },
                )

                SplitType.EXACT -> InputRow(
                    name = name,
                    text = form.exactTexts[member.id].orEmpty(),
                    prefix = "₹",
                    suffix = null,
                    description = stringResource(R.string.editor_amount_for, name),
                    onChange = { actions.onExactChange(member.id, it) },
                    computedPaise = null,
                )

                SplitType.PERCENT -> InputRow(
                    name = name,
                    text = form.percentTexts[member.id].orEmpty(),
                    prefix = null,
                    suffix = "%",
                    description = stringResource(R.string.editor_percent_for, name),
                    onChange = { actions.onPercentChange(member.id, it) },
                    computedPaise = result.sharesPaise[member.id],
                )
            }
        }
    }
}

@Composable
private fun EqualRow(name: String, included: Boolean, sharePaise: Long?, onToggle: () -> Unit) {
    val colors = SplitEasyTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = included, role = Role.Checkbox, onValueChange = { onToggle() })
            .heightIn(min = 52.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(
            checked = included,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = colors.primary,
                uncheckedColor = colors.muted,
            ),
        )
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        if (included && sharePaise != null) {
            AmountText(sharePaise, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
        }
    }
}

@Composable
private fun InputRow(
    name: String,
    text: String,
    prefix: String?,
    suffix: String?,
    description: String,
    onChange: (String) -> Unit,
    computedPaise: Long?,
) {
    val colors = SplitEasyTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(start = 14.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
            if (computedPaise != null) {
                Text(
                    formatInr(computedPaise),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = onChange,
            singleLine = true,
            prefix = prefix?.let { { Text(it) } },
            suffix = suffix?.let { { Text(it) } },
            placeholder = {
                Text("0", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.End,
                fontFeatureSettings = TABULAR_NUMBERS,
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = colors.border,
                focusedBorderColor = colors.primary,
            ),
            modifier = Modifier
                .width(132.dp)
                .semantics { contentDescription = description },
        )
    }
}

/** "✓ ₹2,400 of ₹2,400 assigned · 5 people", or what's still missing. */
@Composable
private fun SplitFooter(status: SplitStatus) {
    val colors = SplitEasyTheme.colors
    val (text, color, ok) = when (status) {
        SplitStatus.NeedsAmount -> Triple(
            stringResource(R.string.editor_status_enter_amount),
            colors.muted,
            false,
        )

        SplitStatus.NoParticipants -> Triple(
            stringResource(R.string.editor_status_no_one),
            colors.owe,
            false,
        )

        SplitStatus.InvalidEntry -> Triple(
            stringResource(R.string.editor_status_invalid),
            colors.owe,
            false,
        )

        is SplitStatus.Balanced -> Triple(
            stringResource(R.string.editor_status_balanced, formatInr(status.totalPaise)),
            colors.onPrimaryTint,
            true,
        )

        is SplitStatus.ExactLeft -> Triple(
            stringResource(R.string.editor_status_left, formatInr(status.paise)),
            colors.owe,
            false,
        )

        is SplitStatus.ExactOver -> Triple(
            stringResource(R.string.editor_status_over, formatInr(status.paise)),
            colors.owe,
            false,
        )

        is SplitStatus.PercentLeft ->
            Triple(
                stringResource(
                    R.string.editor_status_percent_left,
                    formatBasisPoints(status.basisPoints),
                ),
                colors.owe,
                false,
            )

        is SplitStatus.PercentOver ->
            Triple(
                stringResource(
                    R.string.editor_status_percent_over,
                    formatBasisPoints(status.basisPoints),
                ),
                colors.owe,
                false,
            )
    }
    Column(Modifier.navigationBarsPadding()) {
        HorizontalDivider(color = colors.border)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (ok) {
                Icon(
                    painterResource(R.drawable.ic_check),
                    null,
                    tint = color,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = color,
                modifier = Modifier.weight(1f),
            )
            if (status is SplitStatus.Balanced) {
                Text(
                    pluralStringResource(
                        R.plurals.editor_people,
                        status.participants,
                        status.participants,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
        }
    }
}
