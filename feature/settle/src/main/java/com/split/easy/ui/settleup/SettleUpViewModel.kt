package com.split.easy.ui.settleup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.money.ParsedAmount
import com.split.easy.domain.money.formatPlainAmount
import com.split.easy.domain.money.parseInrToPaise
import com.split.easy.domain.repository.GroupRepository
import com.split.easy.domain.repository.MemberRepository
import com.split.easy.domain.repository.SettlementRepository
import com.split.easy.domain.settlement.AmountCheck
import com.split.easy.domain.settlement.SettlementRules
import com.split.easy.domain.upi.UpiPayment
import com.split.easy.domain.upi.UpiResult
import com.split.easy.domain.usecase.ObserveGroupBalances
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What happened after the last UPI attempt, shown under the Pay button. */
enum class PayOutcome {
    NONE,

    /** FAILURE or no response: offer "Payment didn't go through? Record payment" (Q23). */
    NOT_COMPLETED,

    /** No app can handle upi:// links. */
    NO_UPI_APP,
}

data class SettleUpUiState(
    val isLoading: Boolean = true,
    val groupName: String = "",
    val payee: Member? = null,
    val me: Member? = null,
    /** What you owe the payee now: the suggestion minus your pending payments (Q4). */
    val payablePaise: Long = 0,
    val amountText: String = "",
    val note: String = "",
    val outcome: PayOutcome = PayOutcome.NONE,
    val isSaving: Boolean = false,
) {
    val amountPaise: Long?
        get() = (parseInrToPaise(amountText) as? ParsedAmount.Valid)?.paise

    /** From 1 paisa up to what's payable, never above the cap (Q5). */
    val amountCheck: AmountCheck?
        get() = amountPaise?.let { SettlementRules.checkAmount(it, payablePaise) }

    val canPayWithUpi: Boolean
        get() = payee?.upiId != null && amountCheck == AmountCheck.OK && !isSaving

    val canRecord: Boolean
        get() = amountCheck == AmountCheck.OK && !isSaving
}

@HiltViewModel
class SettleUpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val settlementRepository: SettlementRepository,
    observeGroupBalances: ObserveGroupBalances,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])
    private val toMemberId: String = checkNotNull(savedStateHandle["toMemberId"])

    private val input = MutableStateFlow(Input())

    private data class Input(
        val amountText: String? = null,
        val note: String? = null,
        val outcome: PayOutcome = PayOutcome.NONE,
        val isSaving: Boolean = false,
    )

    private val done = Channel<Unit>(Channel.BUFFERED)

    /** Emits once a payment is recorded, so the sheet can close. */
    val doneEvents = done.receiveAsFlow()

    val uiState: StateFlow<SettleUpUiState> = combine(
        groupRepository.observeGroup(groupId),
        observeGroupBalances(groupId),
        input,
    ) { group, balances, input ->
        val me = balances.members.firstOrNull { it.id == balances.myMemberId }
        val payable = me?.let { balances.payable(it.id, toMemberId) } ?: 0L
        val groupName = group?.name.orEmpty()
        SettleUpUiState(
            isLoading = false,
            groupName = groupName,
            payee = balances.members.firstOrNull { it.id == toMemberId },
            me = me,
            payablePaise = payable,
            // Starts at the full balance (06); the user can lower it.
            amountText = input.amountText ?: if (payable > 0) formatPlainAmount(payable) else "",
            note = input.note ?: "SplitEasy · $groupName",
            outcome = input.outcome,
            isSaving = input.isSaving,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettleUpUiState())

    fun onAmountChange(text: String) = input.update {
        it.copy(
            amountText = text.filter { c ->
                c.isDigit() || c == '.'
            },
            outcome = PayOutcome.NONE,
        )
    }

    fun onNoteChange(text: String) = input.update { it.copy(note = text.take(50)) }

    /** The link to hand to the UPI app chooser, or null if Pay isn't possible. */
    fun upiUri(): String? {
        val state = uiState.value
        val payee = state.payee ?: return null
        val upiId = payee.upiId ?: return null
        val amount = state.amountPaise ?: return null
        if (!state.canPayWithUpi) return null
        return UpiPayment.buildUri(upiId, payee.displayName, amount, state.note)
    }

    fun onNoUpiApp() = input.update { it.copy(outcome = PayOutcome.NO_UPI_APP) }

    /** Called with the UPI app's "response" extra when the user comes back (FR-9). */
    fun onUpiResult(response: String?) {
        when (val result = UpiPayment.parseResponse(response)) {
            is UpiResult.Success -> record(SettlementMethod.UPI, result.transactionId)

            UpiResult.Failure, UpiResult.NoResponse -> input.update {
                it.copy(outcome = PayOutcome.NOT_COMPLETED)
            }
        }
    }

    /** "Paid in cash? Record payment" (Q2). */
    fun onRecordCash() = record(SettlementMethod.CASH, upiTxnId = null)

    /** Sets a guest payee's UPI ID right here (M2-Q6, F1); it's logged in the feed. */
    fun onSetGuestUpiId(upiId: String) {
        val state = uiState.value
        val payee = state.payee ?: return
        val me = state.me ?: return
        viewModelScope.launch { memberRepository.setGuestUpiId(payee.id, me.id, upiId) }
    }

    private fun record(method: SettlementMethod, upiTxnId: String?) {
        val state = uiState.value
        val me = state.me ?: return
        val payee = state.payee ?: return
        val amount = state.amountPaise ?: return
        // You're the payer, so: pending until the payee confirms, or confirmed for a guest (Q2, Q3).
        val status =
            SettlementRules.initialStatus(recorder = me, payer = me, payee = payee) ?: return
        if (state.isSaving) return
        input.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            settlementRepository.recordSettlement(
                groupId,
                me.id,
                payee.id,
                amount,
                method,
                status,
                upiTxnId,
            )
            done.send(Unit)
        }
    }

    /** Exposed for tests: the status a payment to this payee starts with. */
    fun initialStatus(): SettlementStatus? {
        val state = uiState.value
        val me = state.me ?: return null
        val payee = state.payee ?: return null
        return SettlementRules.initialStatus(me, me, payee)
    }
}
