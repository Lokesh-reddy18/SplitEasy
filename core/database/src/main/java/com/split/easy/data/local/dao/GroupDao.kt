package com.split.easy.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.split.easy.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Upsert
    suspend fun upsert(group: GroupEntity)

    /** Groups on the home screen: not archived, not deleted, most recent activity first (M2-Q4). */
    @Query(
        "SELECT * FROM groups WHERE isDeleted = 0 AND isArchived = 0 " +
            "ORDER BY lastActivityAt DESC, name COLLATE NOCASE",
    )
    fun observeActiveGroups(): Flow<List<GroupEntity>>

    @Query(
        "SELECT * FROM groups WHERE isDeleted = 0 AND isArchived = 1 " +
            "ORDER BY lastActivityAt DESC, name COLLATE NOCASE",
    )
    fun observeArchivedGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id AND isDeleted = 0")
    fun observeGroup(id: String): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroup(id: String): GroupEntity?

    @Query(
        "UPDATE groups SET name = :name, updatedAt = :updatedAt, syncState = 'PENDING' " +
            "WHERE id = :id",
    )
    suspend fun rename(id: String, name: String, updatedAt: Long)

    /** Re-uploads the group without changing updatedAt, so it can't win a conflict it shouldn't. */
    @Query("UPDATE groups SET syncState = 'PENDING' WHERE id = :id")
    suspend fun markPending(id: String)

    /**
     * Moves the group's activity time forward (never back). Leaves updatedAt and syncState alone;
     * in M4 this field syncs on its own, keeping the larger value.
     */
    @Query("UPDATE groups SET lastActivityAt = MAX(lastActivityAt, :at) WHERE id = :id")
    suspend fun touchActivity(id: String, at: Long)

    @Query(
        "UPDATE groups SET isArchived = :isArchived, updatedAt = :updatedAt, " +
            "syncState = 'PENDING' WHERE id = :id",
    )
    suspend fun setArchived(id: String, isArchived: Boolean, updatedAt: Long)

    @Query(
        "UPDATE groups SET isDeleted = 1, updatedAt = :updatedAt, syncState = 'PENDING' " +
            "WHERE id = :id",
    )
    suspend fun softDelete(id: String, updatedAt: Long)
}
