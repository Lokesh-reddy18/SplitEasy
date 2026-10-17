package com.split.easy.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.split.easy.data.local.entity.SettlementEntity
import com.split.easy.domain.model.SettlementStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementDao {

    @Upsert
    suspend fun upsert(settlement: SettlementEntity)

    /** All non-deleted settlements in a group, newest first (pending ones included). */
    @Query(
        "SELECT * FROM settlements WHERE groupId = :groupId AND isDeleted = 0 " +
            "ORDER BY date DESC, updatedAt DESC",
    )
    fun observeSettlements(groupId: String): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE id = :id")
    suspend fun getSettlement(id: String): SettlementEntity?

    /** Confirm or reject a pending settlement. */
    @Query(
        "UPDATE settlements SET status = :status, updatedAt = :updatedAt, syncState = 'PENDING' " +
            "WHERE id = :id",
    )
    suspend fun updateStatus(id: String, status: SettlementStatus, updatedAt: Long)

    /** Cancel a pending settlement or undo a confirmed one. */
    @Query(
        "UPDATE settlements SET isDeleted = 1, updatedAt = :updatedAt, syncState = 'PENDING' " +
            "WHERE id = :id",
    )
    suspend fun softDelete(id: String, updatedAt: Long)

    /** Per group: how many payments are waiting for this user to confirm (badge on Groups home). */
    @Query(
        """
        SELECT s.groupId AS groupId, COUNT(*) AS count
        FROM settlements s
        JOIN members m ON m.id = s.toMemberId
        WHERE m.userId = :userId AND s.status = 'PENDING' AND s.isDeleted = 0
        GROUP BY s.groupId
        """,
    )
    fun observeWaitingForConfirmation(userId: String): Flow<List<GroupCount>>
}
