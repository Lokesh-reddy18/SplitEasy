package com.split.easy.domain.model

/** An append-only entry in the group feed, such as "Arjun set Ravi's UPI ID to …" (F10). */
data class GroupEvent(
    val id: String,
    val groupId: String,
    val actorMemberId: String,
    val targetMemberId: String,
    val type: GroupEventType,
    /** Null the first time a guest's UPI ID is set (H5). */
    val oldValue: String?,
    val newValue: String,
    val createdAt: Long,
)
