package com.split.easy.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.split.easy.data.local.entity.ExpenseShareEntity
import com.split.easy.data.local.entity.GroupEntity
import com.split.easy.data.local.entity.GroupEventEntity
import com.split.easy.data.local.entity.MemberEntity
import com.split.easy.data.local.entity.SettlementEntity

/**
 * Queries for the sync worker (FR-10). Rows are marked SYNCED only if they haven't changed since
 * they were read: an edit made during an upload keeps its PENDING state and goes up next time.
 */
@Dao
interface SyncDao {

    @Query("SELECT * FROM groups WHERE syncState = 'PENDING'")
    suspend fun pendingGroups(): List<GroupEntity>

    @Query("SELECT * FROM members WHERE syncState = 'PENDING'")
    suspend fun pendingMembers(): List<MemberEntity>

    @Transaction
    @Query("SELECT * FROM expenses WHERE syncState = 'PENDING'")
    suspend fun pendingExpenses(): List<ExpenseWithShares>

    @Query("SELECT * FROM settlements WHERE syncState = 'PENDING'")
    suspend fun pendingSettlements(): List<SettlementEntity>

    @Query("SELECT * FROM group_events WHERE syncState = 'PENDING'")
    suspend fun pendingEvents(): List<GroupEventEntity>

    /** Signed-in users in a group, current members only: who may read it in Firestore. */
    @Query(
        "SELECT DISTINCT userId FROM members WHERE groupId = :groupId AND userId IS NOT NULL AND isDeleted = 0",
    )
    suspend fun memberUserIds(groupId: String): List<String>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun group(id: String): GroupEntity?

    @Query("SELECT * FROM expense_shares WHERE expenseId = :expenseId")
    suspend fun shares(expenseId: String): List<ExpenseShareEntity>

    @Query("UPDATE groups SET syncState = 'SYNCED' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markGroupSynced(id: String, updatedAt: Long)

    @Query("UPDATE members SET syncState = 'SYNCED' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markMemberSynced(id: String, updatedAt: Long)

    @Query("UPDATE expenses SET syncState = 'SYNCED' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markExpenseSynced(id: String, updatedAt: Long)

    @Query("UPDATE settlements SET syncState = 'SYNCED' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSettlementSynced(id: String, updatedAt: Long)

    /** Events are never edited, so no timestamp check is needed. */
    @Query("UPDATE group_events SET syncState = 'SYNCED' WHERE id = :id")
    suspend fun markEventSynced(id: String)
}
