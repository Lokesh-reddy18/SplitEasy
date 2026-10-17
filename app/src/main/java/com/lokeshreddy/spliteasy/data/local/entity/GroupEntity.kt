package com.lokeshreddy.spliteasy.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "groups", indices = [Index("syncState")])
data class GroupEntity(
    /** Client-generated UUID. */
    @PrimaryKey val id: String,
    val name: String,
    /** Firebase Auth uid of the creator. */
    val createdBy: String,
    val isArchived: Boolean = false,
    /** Epoch milliseconds; last write wins during sync. */
    val updatedAt: Long,
    val syncState: SyncState = SyncState.PENDING,
    val isDeleted: Boolean = false,
)
