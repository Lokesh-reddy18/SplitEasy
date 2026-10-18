package com.split.easy.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A person in a group. Guests have no [userId]. */
@Entity(
    tableName = "members",
    indices = [Index("groupId"), Index("userId"), Index("syncState")],
)
data class MemberEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val displayName: String,
    /** Firebase Auth uid; null for guests. */
    val userId: String? = null,
    /** Editable by the owner; for guests, by any member, with each change logged (F1). */
    val upiId: String? = null,
    /** Join time in epoch milliseconds. Member order = createdAt, then id (M1-Q2). */
    val createdAt: Long,
    val updatedAt: Long,
    val syncState: SyncState = SyncState.PENDING,
    /** True once the member is removed or leaves. Their old expenses keep showing their name. */
    val isDeleted: Boolean = false,
)
