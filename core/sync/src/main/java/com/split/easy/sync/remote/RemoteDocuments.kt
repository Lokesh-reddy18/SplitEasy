package com.split.easy.sync.remote

// What each Firestore document holds (see docs/plans/M4-auth-sync.md §4).
//
// These are plain Kotlin classes with only strings, numbers and booleans, so the sync logic can be
// tested without Firebase. Enums are stored by name, dates as ISO strings ("2026-10-10"), and
// times as epoch milliseconds, the same as Room.

/** `groups/{id}` */
data class RemoteGroup(
    val id: String,
    val name: String,
    val createdBy: String,
    /** Signed-in members' uids: who may read the group, and how a phone finds its groups. */
    val memberUserIds: List<String>,
    val isArchived: Boolean,
    val lastActivityAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
)

/** `groups/{groupId}/members/{id}` */
data class RemoteMember(
    val id: String,
    val groupId: String,
    val displayName: String,
    val userId: String?,
    val upiId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
)

/** One share inside an expense document. */
data class RemoteShare(val paise: Long, val basisPoints: Int?)

/** `groups/{groupId}/expenses/{id}`; the shares live inside it, keyed by member id. */
data class RemoteExpense(
    val id: String,
    val groupId: String,
    val paidBy: String,
    val amountPaise: Long,
    val description: String,
    val category: String,
    val date: String,
    val splitType: String,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val shares: Map<String, RemoteShare>,
)

/** `groups/{groupId}/settlements/{id}` */
data class RemoteSettlement(
    val id: String,
    val groupId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amountPaise: Long,
    val date: String,
    val upiTxnId: String?,
    val status: String,
    val method: String,
    val updatedAt: Long,
    val isDeleted: Boolean,
)

/** `groups/{groupId}/events/{id}`: append-only guest UPI ID changes. */
data class RemoteEvent(
    val id: String,
    val groupId: String,
    val actorMemberId: String,
    val targetMemberId: String,
    val type: String,
    val oldValue: String?,
    val newValue: String,
    val createdAt: Long,
)
