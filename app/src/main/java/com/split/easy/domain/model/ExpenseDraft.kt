package com.split.easy.domain.model

import java.time.LocalDate

/** What the Add or Edit expense screen saves. The repository fills in ids, author and timestamps. */
data class ExpenseDraft(
    val groupId: String,
    val paidByMemberId: String,
    val amountPaise: Long,
    val description: String,
    val category: Category,
    val date: LocalDate,
    val splitType: SplitType,
    val sharesPaise: Map<String, Long>,
    val percentBasisPoints: Map<String, Int> = emptyMap(),
)
