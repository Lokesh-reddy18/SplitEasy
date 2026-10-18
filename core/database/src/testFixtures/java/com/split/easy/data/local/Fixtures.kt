package com.split.easy.data.local

import com.split.easy.data.local.entity.ExpenseEntity
import com.split.easy.data.local.entity.GroupEntity
import com.split.easy.data.local.entity.MemberEntity
import com.split.easy.data.local.entity.SettlementEntity
import com.split.easy.data.local.entity.SyncState
import com.split.easy.domain.model.Category
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.model.SplitType
import java.time.LocalDate

val OCT_10: LocalDate = LocalDate.of(2026, 10, 10)

fun group(
    id: String,
    name: String = id,
    isArchived: Boolean = false,
    isDeleted: Boolean = false,
    syncState: SyncState = SyncState.PENDING,
) = GroupEntity(
    id = id,
    name = name,
    createdBy = "uid-you",
    isArchived = isArchived,
    updatedAt = 1L,
    syncState = syncState,
    isDeleted = isDeleted,
)

fun member(
    id: String,
    groupId: String = "g1",
    userId: String? = null,
    createdAt: Long = 1L,
    isDeleted: Boolean = false,
    syncState: SyncState = SyncState.PENDING,
) = MemberEntity(
    id = id,
    groupId = groupId,
    displayName = id,
    userId = userId,
    createdAt = createdAt,
    updatedAt = createdAt,
    syncState = syncState,
    isDeleted = isDeleted,
)

fun expense(
    id: String,
    groupId: String = "g1",
    date: LocalDate = OCT_10,
    createdAt: Long = 1L,
    isDeleted: Boolean = false,
    syncState: SyncState = SyncState.PENDING,
) = ExpenseEntity(
    id = id,
    groupId = groupId,
    paidBy = "priya",
    amountPaise = 2_40_000,
    description = "Dinner at Raintree",
    category = Category.FOOD,
    date = date,
    splitType = SplitType.EQUAL,
    createdBy = "uid-you",
    createdAt = createdAt,
    updatedAt = createdAt,
    syncState = syncState,
    isDeleted = isDeleted,
)

fun settlement(
    id: String,
    from: String,
    to: String,
    groupId: String = "g1",
    status: SettlementStatus = SettlementStatus.PENDING,
    isDeleted: Boolean = false,
    syncState: SyncState = SyncState.PENDING,
) = SettlementEntity(
    id = id,
    groupId = groupId,
    fromMemberId = from,
    toMemberId = to,
    amountPaise = 1_20_000,
    date = OCT_10,
    upiTxnId = "481022756631",
    status = status,
    method = SettlementMethod.UPI,
    updatedAt = 1L,
    syncState = syncState,
    isDeleted = isDeleted,
)
