package com.split.easy.domain

import com.split.easy.domain.model.Category
import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.Settlement
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.model.SplitType
import java.time.LocalDate

/** Test builders that fill in fields a test doesn't care about. */
val TEST_DATE: LocalDate = LocalDate.of(2026, 10, 10)

fun testMember(id: String, userId: String?, isRemoved: Boolean = false) =
    Member(id = id, displayName = id, userId = userId, isRemoved = isRemoved)

fun testExpense(
    id: String,
    paidBy: String,
    amountPaise: Long,
    sharesPaise: Map<String, Long>,
    isDeleted: Boolean = false,
) = Expense(
    id = id,
    groupId = "g1",
    paidByMemberId = paidBy,
    amountPaise = amountPaise,
    description = id,
    category = Category.OTHER,
    date = TEST_DATE,
    splitType = SplitType.EXACT,
    sharesPaise = sharesPaise,
    createdByUserId = "uid-test",
    createdAt = 0L,
    isDeleted = isDeleted,
)

fun testSettlement(
    from: String,
    to: String,
    amountPaise: Long,
    status: SettlementStatus = SettlementStatus.CONFIRMED,
    method: SettlementMethod = SettlementMethod.UPI,
    isDeleted: Boolean = false,
    id: String = "$from-$to-$amountPaise-$status",
) = Settlement(
    id = id,
    groupId = "g1",
    fromMemberId = from,
    toMemberId = to,
    amountPaise = amountPaise,
    status = status,
    method = method,
    date = TEST_DATE,
    isDeleted = isDeleted,
)
