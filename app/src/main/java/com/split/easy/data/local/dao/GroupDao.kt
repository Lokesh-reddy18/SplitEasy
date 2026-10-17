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

    /** Groups on the home screen: not archived, not deleted. */
    @Query(
        "SELECT * FROM groups WHERE isDeleted = 0 AND isArchived = 0 ORDER BY name COLLATE NOCASE",
    )
    fun observeActiveGroups(): Flow<List<GroupEntity>>

    @Query(
        "SELECT * FROM groups WHERE isDeleted = 0 AND isArchived = 1 ORDER BY name COLLATE NOCASE",
    )
    fun observeArchivedGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id AND isDeleted = 0")
    fun observeGroup(id: String): Flow<GroupEntity?>

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
