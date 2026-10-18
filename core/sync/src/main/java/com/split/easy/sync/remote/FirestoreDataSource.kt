package com.split.easy.sync.remote

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.split.easy.sync.remote.FirestoreFields.toMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

/** Firestore paths and queries (layout in docs/plans/M4-auth-sync.md §4). */
@Singleton
class FirestoreDataSource @Inject constructor(private val firestore: FirebaseFirestore) :
    RemoteDataSource {

    private val groups get() = firestore.collection(GROUPS)

    private fun CollectionReference.inGroup(groupId: String, name: String) =
        document(groupId).collection(name)

    override suspend fun groupsFor(userId: String): List<RemoteGroup> =
        groups.whereArrayContains(MEMBER_USER_IDS, userId).get().await().documents
            .mapNotNull { it.decode(FirestoreFields::group) }

    override suspend fun changesIn(groupId: String, sinceMillis: Long): RemoteGroupContents {
        suspend fun changed(name: String, timeField: String) = groups.inGroup(
            groupId,
            name,
        ).whereGreaterThan(timeField, sinceMillis).get().await().documents
        return RemoteGroupContents(
            members = changed(MEMBERS, UPDATED_AT).mapNotNull {
                it.decode(FirestoreFields::member)
            },
            expenses = changed(EXPENSES, UPDATED_AT).mapNotNull {
                it.decode(FirestoreFields::expense)
            },
            settlements = changed(SETTLEMENTS, UPDATED_AT).mapNotNull {
                it.decode(FirestoreFields::settlement)
            },
            events = changed(EVENTS, CREATED_AT).mapNotNull { it.decode(FirestoreFields::event) },
        )
    }

    override fun observe(groupId: String): Flow<RemoteGroupContents> = combine(
        groups.inGroup(groupId, MEMBERS).snapshots(FirestoreFields::member),
        groups.inGroup(groupId, EXPENSES).snapshots(FirestoreFields::expense),
        groups.inGroup(groupId, SETTLEMENTS).snapshots(FirestoreFields::settlement),
        groups.inGroup(groupId, EVENTS).snapshots(FirestoreFields::event),
    ) { members, expenses, settlements, events ->
        RemoteGroupContents(members, expenses, settlements, events)
    }

    override fun observeGroupsFor(userId: String): Flow<List<RemoteGroup>> =
        groups.whereArrayContains(MEMBER_USER_IDS, userId).snapshots(FirestoreFields::group)

    override suspend fun write(batch: RemoteWrite) {
        require(batch.size <= RemoteDataSource.MAX_BATCH_SIZE) { "Batch too large: ${batch.size}" }
        if (batch.isEmpty()) return
        val writes = firestore.batch()
        batch.groups.forEach { writes.set(groups.document(it.id), it.toMap()) }
        batch.members.forEach {
            writes.set(groups.inGroup(it.groupId, MEMBERS).document(it.id), it.toMap())
        }
        batch.expenses.forEach {
            writes.set(groups.inGroup(it.groupId, EXPENSES).document(it.id), it.toMap())
        }
        batch.settlements.forEach {
            writes.set(groups.inGroup(it.groupId, SETTLEMENTS).document(it.id), it.toMap())
        }
        batch.events.forEach {
            writes.set(groups.inGroup(it.groupId, EVENTS).document(it.id), it.toMap())
        }
        writes.commit().await()
    }

    /** Live query results, decoded; damaged documents are skipped. */
    private fun <T : Any> Query.snapshots(decode: (String, Map<String, Any?>) -> T): Flow<List<T>> =
        callbackFlow {
            val registration = addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.decode(decode) })
                }
            }
            awaitClose { registration.remove() }
        }

    private fun <T : Any> DocumentSnapshot.decode(decode: (String, Map<String, Any?>) -> T): T? =
        data?.let { runCatching { decode(id, it) }.getOrNull() }

    private companion object {
        const val GROUPS = "groups"
        const val MEMBERS = "members"
        const val EXPENSES = "expenses"
        const val SETTLEMENTS = "settlements"
        const val EVENTS = "events"
        const val MEMBER_USER_IDS = "memberUserIds"
        const val UPDATED_AT = "updatedAt"
        const val CREATED_AT = "createdAt"
    }
}
