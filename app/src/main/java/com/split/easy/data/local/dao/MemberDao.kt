package com.split.easy.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.split.easy.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

/** Members are always returned in member order: join time, then id (M1-Q2). */
@Dao
interface MemberDao {

    @Upsert
    suspend fun upsert(member: MemberEntity)

    @Upsert
    suspend fun upsertAll(members: List<MemberEntity>)

    /** Current members, for new expenses (H4). */
    @Query(
        "SELECT * FROM members WHERE groupId = :groupId AND isDeleted = 0 ORDER BY createdAt, id",
    )
    fun observeCurrentMembers(groupId: String): Flow<List<MemberEntity>>

    /** Everyone who was ever in the group, so old expenses can still show removed members. */
    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY createdAt, id")
    fun observeAllMembers(groupId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMember(id: String): MemberEntity?

    @Query(
        "UPDATE members SET upiId = :upiId, updatedAt = :updatedAt, syncState = 'PENDING' " +
            "WHERE id = :id",
    )
    suspend fun updateUpiId(id: String, upiId: String?, updatedAt: Long)

    /** The user's member row in every group they're in (used to update their UPI ID, F2). */
    @Query("SELECT * FROM members WHERE userId = :userId AND isDeleted = 0")
    suspend fun getMembershipsOfUser(userId: String): List<MemberEntity>

    @Query(
        "UPDATE members SET isDeleted = 1, updatedAt = :updatedAt, syncState = 'PENDING' " +
            "WHERE id = :id",
    )
    suspend fun softDelete(id: String, updatedAt: Long)
}
