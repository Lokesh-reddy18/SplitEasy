package com.split.easy.domain.model

import java.time.LocalDate

/** A payment from one member to another to settle a debt. Money is in paise. */
data class Settlement(
    val id: String,
    val groupId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amountPaise: Long,
    val status: SettlementStatus,
    val method: SettlementMethod,
    val date: LocalDate,
    val upiTxnId: String? = null,
    val updatedAt: Long = 0L,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
) {
    /** Only confirmed, non-deleted settlements change balances. */
    val countsTowardBalances: Boolean
        get() = status == SettlementStatus.CONFIRMED && !isDeleted

    val isOpenPending: Boolean
        get() = status == SettlementStatus.PENDING && !isDeleted
}
