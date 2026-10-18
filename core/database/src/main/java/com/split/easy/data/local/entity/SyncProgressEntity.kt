package com.split.easy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * When each group was last downloaded from Firestore, so the next catch-up fetches only newer
 * changes. Stored in Room so signing out (which clears every table) also forgets it.
 */
@Entity(tableName = "sync_progress")
data class SyncProgressEntity(
    @PrimaryKey val groupId: String,
    /** Newest updatedAt (or createdAt, for events) seen from Firestore, in epoch milliseconds. */
    val lastDownloadedAt: Long,
)
