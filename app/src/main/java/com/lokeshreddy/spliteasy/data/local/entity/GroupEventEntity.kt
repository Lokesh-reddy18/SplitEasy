package com.lokeshreddy.spliteasy.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lokeshreddy.spliteasy.domain.model.GroupEventType

/**
 * Append-only log entry shown in the group feed, such as a change to a guest's UPI ID (F10, H1).
 * Never edited or deleted, so it has no updatedAt or isDeleted.
 */
@Entity(tableName = "group_events", indices = [Index("groupId"), Index("syncState")])
data class GroupEventEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val actorMemberId: String,
    val targetMemberId: String,
    val type: GroupEventType,
    /** Null when a guest's UPI ID is set for the first time. */
    val oldValue: String? = null,
    val newValue: String,
    val createdAt: Long,
    val syncState: SyncState = SyncState.PENDING,
)
