package com.lokeshreddy.spliteasy.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lokeshreddy.spliteasy.domain.model.Category
import com.lokeshreddy.spliteasy.domain.model.SplitType
import java.time.LocalDate

@Entity(tableName = "expenses", indices = [Index("groupId"), Index("syncState")])
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    /** Member id of the payer. */
    val paidBy: String,
    val amountPaise: Long,
    val description: String,
    val category: Category,
    /** Date only, no time (the designs show dates). */
    val date: LocalDate,
    val splitType: SplitType,
    /** Firebase Auth uid of the person who added it; only they can edit it (Q11). */
    val createdBy: String,
    /** Orders expenses on the same day, newest first (M1-Q5). */
    val createdAt: Long,
    val updatedAt: Long,
    val syncState: SyncState = SyncState.PENDING,
    val isDeleted: Boolean = false,
)
