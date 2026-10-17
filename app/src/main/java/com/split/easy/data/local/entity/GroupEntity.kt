package com.split.easy.data.local.entity

import androidx.room.ColumnInfo
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
    /**
     * Latest expense, settlement or group change, in epoch milliseconds (M2-Q4). Orders Groups home.
     * Bumping it doesn't change [updatedAt], so adding an expense can't overwrite a rename made on
     * another phone under last-write-wins.
     */
    @ColumnInfo(defaultValue = "0")
    val lastActivityAt: Long = 0L,
    /** Epoch milliseconds; last write wins during sync. */
    val updatedAt: Long,
    val syncState: SyncState = SyncState.PENDING,
    val isDeleted: Boolean = false,
)
