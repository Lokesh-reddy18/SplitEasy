package com.split.easy.domain.model

data class Group(
    val id: String,
    val name: String,
    /** Firebase Auth uid of the person who created the group. */
    val createdByUserId: String,
    val isArchived: Boolean = false,
    /** Latest expense, settlement or group change; orders Groups home (M2-Q4). */
    val lastActivityAt: Long = 0L,
    val isSynced: Boolean = false,
)
