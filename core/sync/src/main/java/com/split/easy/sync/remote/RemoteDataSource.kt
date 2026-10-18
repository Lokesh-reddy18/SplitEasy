package com.split.easy.sync.remote

import kotlinx.coroutines.flow.Flow

/** One group's documents, as read from Firestore. */
data class RemoteGroupContents(
    val members: List<RemoteMember>,
    val expenses: List<RemoteExpense>,
    val settlements: List<RemoteSettlement>,
    val events: List<RemoteEvent>,
)

/** A batch of documents to write together: all of it lands, or none of it does. */
data class RemoteWrite(
    val groups: List<RemoteGroup> = emptyList(),
    val members: List<RemoteMember> = emptyList(),
    val expenses: List<RemoteExpense> = emptyList(),
    val settlements: List<RemoteSettlement> = emptyList(),
    val events: List<RemoteEvent> = emptyList(),
) {
    val size: Int
        get() = groups.size + members.size + expenses.size + settlements.size + events.size

    fun isEmpty() = size == 0
}

/**
 * The sync code's only view of the server. Upload and download use this interface, so their
 * logic is tested with an in-memory fake; [FirestoreDataSource] is the real implementation.
 */
interface RemoteDataSource {
    /** Groups this user is a member of (archived ones included). */
    suspend fun groupsFor(userId: String): List<RemoteGroup>

    /** Everything in one group that changed after [sinceMillis] (0 for everything). */
    suspend fun changesIn(groupId: String, sinceMillis: Long): RemoteGroupContents

    /** Live updates for one group while the app is open; emits whenever something changes. */
    fun observe(groupId: String): Flow<RemoteGroupContents>

    /** Live list of the user's groups, so new or archived groups are noticed. */
    fun observeGroupsFor(userId: String): Flow<List<RemoteGroup>>

    /** Writes everything in one Firestore batch (at most [MAX_BATCH_SIZE] documents). */
    suspend fun write(batch: RemoteWrite)

    companion object {
        /** Firestore's limit on writes per batch. */
        const val MAX_BATCH_SIZE = 500
    }
}
