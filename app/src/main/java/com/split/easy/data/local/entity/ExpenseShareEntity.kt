package com.split.easy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * One participant's share of an expense. Shares are always written together with their expense
 * (and synced inside the expense's Firestore document), so this is the only table with a
 * foreign key. Other tables use plain groupId columns because sync can deliver records in any order.
 */
@Entity(
    tableName = "expense_shares",
    primaryKeys = ["expenseId", "memberId"],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("memberId")],
)
data class ExpenseShareEntity(
    val expenseId: String,
    val memberId: String,
    val sharePaise: Long,
    /** The percentage as entered (33.33% = 3333), kept only for percentage splits (M1-Q3). */
    val percentBasisPoints: Int? = null,
)
