package com.split.easy.domain.model

import java.time.LocalDate

/** An expense in a group. Money is in paise. */
data class Expense(
    val id: String,
    val groupId: String,
    val paidByMemberId: String,
    val amountPaise: Long,
    val description: String,
    val category: Category,
    val date: LocalDate,
    val splitType: SplitType,
    /** Each participant's share. Shares add up to [amountPaise]; screens list them in member order. */
    val sharesPaise: Map<String, Long>,
    /** The percentages as entered (33.33% = 3333), only for percentage splits (M1-Q3). */
    val percentBasisPoints: Map<String, Int> = emptyMap(),
    /** Firebase Auth uid of the person who added it; only they can edit it (Q11). */
    val createdByUserId: String,
    val createdAt: Long,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
)
