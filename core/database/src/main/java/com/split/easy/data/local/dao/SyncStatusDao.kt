package com.split.easy.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStatusDao {

    /** Local changes not uploaded yet, for "2 changes waiting to sync". */
    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM groups WHERE syncState = 'PENDING') +
            (SELECT COUNT(*) FROM members WHERE syncState = 'PENDING') +
            (SELECT COUNT(*) FROM expenses WHERE syncState = 'PENDING') +
            (SELECT COUNT(*) FROM settlements WHERE syncState = 'PENDING') +
            (SELECT COUNT(*) FROM group_events WHERE syncState = 'PENDING')
        """,
    )
    fun observePendingChangeCount(): Flow<Int>
}
