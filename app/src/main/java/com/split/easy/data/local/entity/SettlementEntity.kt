package com.split.easy.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import java.time.LocalDate

@Entity(
    tableName = "settlements",
    indices = [Index("groupId"), Index("toMemberId"), Index("syncState")],
)
data class SettlementEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amountPaise: Long,
    val date: LocalDate,
    /** Transaction ID reported by the UPI app, if any. */
    val upiTxnId: String? = null,
    val status: SettlementStatus,
    /** UPI or cash; decides the payee's confirmation message (M1-Q4). */
    val method: SettlementMethod,
    val updatedAt: Long,
    val syncState: SyncState = SyncState.PENDING,
    /** Soft delete: a cancelled pending payment or an undone confirmed one (M1-Q1). */
    val isDeleted: Boolean = false,
)
