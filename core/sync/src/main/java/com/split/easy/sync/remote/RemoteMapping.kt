package com.split.easy.sync.remote

import com.split.easy.data.local.entity.ExpenseEntity
import com.split.easy.data.local.entity.ExpenseShareEntity
import com.split.easy.data.local.entity.GroupEntity
import com.split.easy.data.local.entity.GroupEventEntity
import com.split.easy.data.local.entity.MemberEntity
import com.split.easy.data.local.entity.SettlementEntity
import com.split.easy.data.local.entity.SyncState
import com.split.easy.domain.model.Category
import com.split.easy.domain.model.GroupEventType
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.model.SplitType
import java.time.LocalDate

// Room ↔ Firestore documents. Everything that arrives from Firestore is stored as SYNCED.

fun GroupEntity.toRemote(memberUserIds: List<String>) = RemoteGroup(
    id = id,
    name = name,
    createdBy = createdBy,
    memberUserIds = memberUserIds.distinct().sorted(),
    isArchived = isArchived,
    lastActivityAt = lastActivityAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun RemoteGroup.toEntity() = GroupEntity(
    id = id,
    name = name,
    createdBy = createdBy,
    isArchived = isArchived,
    lastActivityAt = lastActivityAt,
    updatedAt = updatedAt,
    syncState = SyncState.SYNCED,
    isDeleted = isDeleted,
)

fun MemberEntity.toRemote() = RemoteMember(
    id = id,
    groupId = groupId,
    displayName = displayName,
    userId = userId,
    upiId = upiId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun RemoteMember.toEntity() = MemberEntity(
    id = id,
    groupId = groupId,
    displayName = displayName,
    userId = userId,
    upiId = upiId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncState = SyncState.SYNCED,
    isDeleted = isDeleted,
)

fun ExpenseEntity.toRemote(shares: List<ExpenseShareEntity>) = RemoteExpense(
    id = id,
    groupId = groupId,
    paidBy = paidBy,
    amountPaise = amountPaise,
    description = description,
    category = category.name,
    date = date.toString(),
    splitType = splitType.name,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    shares = shares.associate { it.memberId to RemoteShare(it.sharePaise, it.percentBasisPoints) },
)

/** The expense and its shares, ready for Room. */
fun RemoteExpense.toEntities(): Pair<ExpenseEntity, List<ExpenseShareEntity>> {
    val expense = ExpenseEntity(
        id = id,
        groupId = groupId,
        paidBy = paidBy,
        amountPaise = amountPaise,
        description = description,
        // An unknown value (from a newer app version) falls back instead of crashing.
        category = enumOrDefault(category, Category.OTHER),
        date = LocalDate.parse(date),
        splitType = enumOrDefault(splitType, SplitType.EXACT),
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncState = SyncState.SYNCED,
        isDeleted = isDeleted,
    )
    val shareEntities = shares.entries.sortedBy { it.key }.map { (memberId, share) ->
        ExpenseShareEntity(id, memberId, share.paise, share.basisPoints)
    }
    return expense to shareEntities
}

fun SettlementEntity.toRemote() = RemoteSettlement(
    id = id,
    groupId = groupId,
    fromMemberId = fromMemberId,
    toMemberId = toMemberId,
    amountPaise = amountPaise,
    date = date.toString(),
    upiTxnId = upiTxnId,
    status = status.name,
    method = method.name,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun RemoteSettlement.toEntity() = SettlementEntity(
    id = id,
    groupId = groupId,
    fromMemberId = fromMemberId,
    toMemberId = toMemberId,
    amountPaise = amountPaise,
    date = LocalDate.parse(date),
    upiTxnId = upiTxnId,
    // A status this version doesn't know is treated as still pending, so it never counts wrongly.
    status = enumOrDefault(status, SettlementStatus.PENDING),
    method = enumOrDefault(method, SettlementMethod.CASH),
    updatedAt = updatedAt,
    syncState = SyncState.SYNCED,
    isDeleted = isDeleted,
)

fun GroupEventEntity.toRemote() = RemoteEvent(
    id = id,
    groupId = groupId,
    actorMemberId = actorMemberId,
    targetMemberId = targetMemberId,
    type = type.name,
    oldValue = oldValue,
    newValue = newValue,
    createdAt = createdAt,
)

fun RemoteEvent.toEntity() = GroupEventEntity(
    id = id,
    groupId = groupId,
    actorMemberId = actorMemberId,
    targetMemberId = targetMemberId,
    type = enumOrDefault(type, GroupEventType.GUEST_UPI_CHANGED),
    oldValue = oldValue,
    newValue = newValue,
    createdAt = createdAt,
    syncState = SyncState.SYNCED,
)

private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
