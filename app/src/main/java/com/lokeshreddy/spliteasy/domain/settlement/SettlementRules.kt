package com.lokeshreddy.spliteasy.domain.settlement

import com.lokeshreddy.spliteasy.domain.model.Member
import com.lokeshreddy.spliteasy.domain.model.Settlement
import com.lokeshreddy.spliteasy.domain.model.SettlementStatus
import com.lokeshreddy.spliteasy.domain.money.MAX_AMOUNT_PAISE

/**
 * Who may record, confirm, cancel or delete a settlement, and what status a new one starts with.
 * Decisions Q2, Q3, Q5, Q6, F3, F4 and M1-Q6 in docs/OPEN_QUESTIONS.md.
 * Archived groups are read-only; check GroupRules before calling these.
 */
object SettlementRules {

    /**
     * The status a new settlement starts with when [recorder] records that [payer] paid [payee],
     * or null if [recorder] isn't allowed to record it. The same rules apply to UPI and cash.
     *
     * - Recorded by the payee: CONFIRMED (they know they were paid).
     * - Recorded by the payer: PENDING until the payee confirms, or CONFIRMED if the payee is a guest.
     * - Recorded by someone else: only allowed between two guests, and CONFIRMED.
     */
    fun initialStatus(recorder: Member, payer: Member, payee: Member): SettlementStatus? {
        val anyoneRemoved = recorder.isRemoved || payer.isRemoved || payee.isRemoved
        if (recorder.isGuest || anyoneRemoved || payer.id == payee.id) return null

        return when (recorder.id) {
            payee.id -> SettlementStatus.CONFIRMED
            payer.id -> if (payee.isGuest) SettlementStatus.CONFIRMED else SettlementStatus.PENDING
            else -> if (payer.isGuest && payee.isGuest) SettlementStatus.CONFIRMED else null
        }
    }

    /** Only the payee can confirm or reject ("Not received"), and only while it's pending. */
    fun canConfirmOrReject(user: Member, settlement: Settlement): Boolean =
        settlement.isOpenPending && user.id == settlement.toMemberId

    /** The payer can cancel their own payment while it's waiting for confirmation (F4). */
    fun canCancel(user: Member, settlement: Settlement): Boolean =
        settlement.isOpenPending && user.id == settlement.fromMemberId

    /**
     * A confirmed settlement can be deleted (undone) by the payer or payee (Q6).
     * Between two guests, any member can delete it, the same people who could record it (M1-Q6).
     */
    fun canDelete(user: Member, settlement: Settlement, payer: Member, payee: Member): Boolean {
        val isConfirmed = settlement.status == SettlementStatus.CONFIRMED && !settlement.isDeleted
        if (!isConfirmed || user.isGuest || user.isRemoved) return false
        val isParty = user.id == settlement.fromMemberId || user.id == settlement.toMemberId
        return isParty || (payer.isGuest && payee.isGuest)
    }

    /** Checks an entered amount: from 1 paisa up to what's payable, and never above the cap. */
    fun checkAmount(amountPaise: Long, payablePaise: Long): AmountCheck = when {
        amountPaise <= 0 -> AmountCheck.NOT_POSITIVE
        amountPaise > MAX_AMOUNT_PAISE -> AmountCheck.ABOVE_LIMIT
        amountPaise > payablePaise -> AmountCheck.ABOVE_PAYABLE
        else -> AmountCheck.OK
    }
}
