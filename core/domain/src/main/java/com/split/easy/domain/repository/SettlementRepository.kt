package com.split.easy.domain.repository

import com.split.easy.domain.model.Settlement
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import kotlinx.coroutines.flow.Flow

interface SettlementRepository {
    /** Non-deleted settlements, newest first, including pending and rejected ones. */
    fun observeSettlements(groupId: String): Flow<List<Settlement>>

    /** Records a payment. The caller decides [status] with SettlementRules. Returns its id. */
    suspend fun recordSettlement(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amountPaise: Long,
        method: SettlementMethod,
        status: SettlementStatus,
        upiTxnId: String? = null,
    ): String

    /** Confirm or reject ("Not received"). */
    suspend fun setStatus(settlementId: String, status: SettlementStatus)

    /** Cancel a pending payment or undo a confirmed one (soft delete). */
    suspend fun deleteSettlement(settlementId: String)

    /** Per group id: how many payments are waiting for the current user to confirm (F7 badge). */
    fun observeWaitingForMyConfirmation(): Flow<Map<String, Int>>
}
