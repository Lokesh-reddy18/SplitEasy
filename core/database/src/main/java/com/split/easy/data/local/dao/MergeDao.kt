package com.split.easy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.split.easy.data.local.entity.ExpenseEntity
import com.split.easy.data.local.entity.ExpenseShareEntity
import com.split.easy.data.local.entity.GroupEntity
import com.split.easy.data.local.entity.GroupEventEntity
import com.split.easy.data.local.entity.MemberEntity
import com.split.easy.data.local.entity.SettlementEntity
import com.split.easy.data.local.entity.SyncProgressEntity

/** Writes for the download side of sync: storing what other devices changed. */
@Dao
interface MergeDao {

    @Query("SELECT * FROM groups WHERE id IN (:ids)")
    suspend fun groups(ids: List<String>): List<GroupEntity>

    @Query("SELECT * FROM members WHERE id IN (:ids)")
    suspend fun members(ids: List<String>): List<MemberEntity>

    @Query("SELECT * FROM expenses WHERE id IN (:ids)")
    suspend fun expenses(ids: List<String>): List<ExpenseEntity>

    @Query("SELECT * FROM settlements WHERE id IN (:ids)")
    suspend fun settlements(ids: List<String>): List<SettlementEntity>

    @Upsert
    suspend fun upsertGroups(groups: List<GroupEntity>)

    @Upsert
    suspend fun upsertMembers(members: List<MemberEntity>)

    @Upsert
    suspend fun upsertSettlements(settlements: List<SettlementEntity>)

    /** Events never change, so one that's already here is left alone. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvents(events: List<GroupEventEntity>)

    @Upsert
    suspend fun upsertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expense_shares WHERE expenseId = :expenseId")
    suspend fun deleteShares(expenseId: String)

    @Insert
    suspend fun insertShares(shares: List<ExpenseShareEntity>)

    /** Replaces an expense and its shares together. */
    @Transaction
    suspend fun replaceExpense(expense: ExpenseEntity, shares: List<ExpenseShareEntity>) {
        upsertExpense(expense)
        deleteShares(expense.id)
        insertShares(shares)
    }

    @Query("SELECT lastDownloadedAt FROM sync_progress WHERE groupId = :groupId")
    suspend fun lastDownloadedAt(groupId: String): Long?

    @Upsert
    suspend fun setProgress(progress: SyncProgressEntity)

    /** Keeps Groups home's order in step with activity from other devices (M2-Q4). */
    @Query("UPDATE groups SET lastActivityAt = MAX(lastActivityAt, :at) WHERE id = :id")
    suspend fun touchActivity(id: String, at: Long)
}
