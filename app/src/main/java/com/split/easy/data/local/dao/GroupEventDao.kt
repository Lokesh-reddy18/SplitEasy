package com.split.easy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.split.easy.data.local.entity.GroupEventEntity
import kotlinx.coroutines.flow.Flow

/** Group events are append-only: this DAO can insert and read, but never update or delete. */
@Dao
interface GroupEventDao {

    /** An event that already exists (e.g. downloaded again by sync) is left as it is. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: GroupEventEntity)

    @Query("SELECT * FROM group_events WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun observeEvents(groupId: String): Flow<List<GroupEventEntity>>
}
