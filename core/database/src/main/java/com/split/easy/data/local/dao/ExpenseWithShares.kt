package com.split.easy.data.local.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.split.easy.data.local.entity.ExpenseEntity
import com.split.easy.data.local.entity.ExpenseShareEntity

data class ExpenseWithShares(
    @Embedded val expense: ExpenseEntity,
    @Relation(parentColumn = "id", entityColumn = "expenseId")
    val shares: List<ExpenseShareEntity>,
)
