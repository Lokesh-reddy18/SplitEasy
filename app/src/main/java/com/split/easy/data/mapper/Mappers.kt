package com.split.easy.data.mapper

import com.split.easy.data.local.dao.ExpenseWithShares
import com.split.easy.data.local.entity.ExpenseShareEntity
import com.split.easy.data.local.entity.GroupEntity
import com.split.easy.data.local.entity.GroupEventEntity
import com.split.easy.data.local.entity.MemberEntity
import com.split.easy.data.local.entity.SettlementEntity
import com.split.easy.data.local.entity.SyncState
import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.ExpenseDraft
import com.split.easy.domain.model.Group
import com.split.easy.domain.model.GroupEvent
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.Settlement

/** Converts Room rows to domain models. Sync details stay in the data layer except "is it synced". */

fun GroupEntity.toDomain() = Group(
    id = id,
    name = name,
    createdByUserId = createdBy,
    isArchived = isArchived,
    lastActivityAt = lastActivityAt,
    isSynced = syncState == SyncState.SYNCED,
)

fun MemberEntity.toDomain() = Member(
    id = id,
    displayName = displayName,
    userId = userId,
    upiId = upiId,
    createdAt = createdAt,
    isRemoved = isDeleted,
)

fun ExpenseWithShares.toDomain(): Expense {
    // Room returns shares in no fixed order; screens list them in member order.
    val orderedShares = shares.sortedBy { it.memberId }
    return Expense(
        id = expense.id,
        groupId = expense.groupId,
        paidByMemberId = expense.paidBy,
        amountPaise = expense.amountPaise,
        description = expense.description,
        category = expense.category,
        date = expense.date,
        splitType = expense.splitType,
        sharesPaise = orderedShares.associate { it.memberId to it.sharePaise },
        percentBasisPoints = orderedShares
            .mapNotNull { share -> share.percentBasisPoints?.let { share.memberId to it } }
            .toMap(),
        createdByUserId = expense.createdBy,
        createdAt = expense.createdAt,
        isSynced = expense.syncState == SyncState.SYNCED,
        isDeleted = expense.isDeleted,
    )
}

fun ExpenseDraft.toShareEntities(expenseId: String): List<ExpenseShareEntity> =
    sharesPaise.map { (memberId, paise) ->
        ExpenseShareEntity(
            expenseId = expenseId,
            memberId = memberId,
            sharePaise = paise,
            percentBasisPoints = percentBasisPoints[memberId],
        )
    }

fun SettlementEntity.toDomain() = Settlement(
    id = id,
    groupId = groupId,
    fromMemberId = fromMemberId,
    toMemberId = toMemberId,
    amountPaise = amountPaise,
    status = status,
    method = method,
    date = date,
    upiTxnId = upiTxnId,
    updatedAt = updatedAt,
    isSynced = syncState == SyncState.SYNCED,
    isDeleted = isDeleted,
)

fun GroupEventEntity.toDomain() = GroupEvent(
    id = id,
    groupId = groupId,
    actorMemberId = actorMemberId,
    targetMemberId = targetMemberId,
    type = type,
    oldValue = oldValue,
    newValue = newValue,
    createdAt = createdAt,
)
