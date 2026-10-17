package com.lokeshreddy.spliteasy.domain.model

/** A payment from one member to another to settle a debt. Money is in paise. */
data class Settlement(
    val id: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amountPaise: Long,
    val status: SettlementStatus,
    val method: SettlementMethod,
    val isDeleted: Boolean = false,
) {
    /** Only confirmed, non-deleted settlements change balances. */
    val countsTowardBalances: Boolean
        get() = status == SettlementStatus.CONFIRMED && !isDeleted

    val isOpenPending: Boolean
        get() = status == SettlementStatus.PENDING && !isDeleted
}
