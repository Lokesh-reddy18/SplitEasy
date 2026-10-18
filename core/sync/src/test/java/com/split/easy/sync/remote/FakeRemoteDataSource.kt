package com.split.easy.sync.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** An in-memory Firestore. Records every batch, and can be told to fail. */
class FakeRemoteDataSource : RemoteDataSource {
    val batches = mutableListOf<RemoteWrite>()
    val groups = MutableStateFlow<Map<String, RemoteGroup>>(emptyMap())
    val contents = MutableStateFlow<Map<String, RemoteGroupContents>>(emptyMap())

    /** Runs inside the next write, before it "commits"; used to simulate edits during an upload. */
    var beforeWrite: (suspend () -> Unit)? = null
    var failWrites = false

    override suspend fun groupsFor(userId: String) = groups.value.values.filter {
        userId in
            it.memberUserIds
    }

    override suspend fun changesIn(groupId: String, sinceMillis: Long): RemoteGroupContents {
        val all = contents.value[groupId] ?: return EMPTY
        return RemoteGroupContents(
            members = all.members.filter { it.updatedAt > sinceMillis },
            expenses = all.expenses.filter { it.updatedAt > sinceMillis },
            settlements = all.settlements.filter { it.updatedAt > sinceMillis },
            events = all.events.filter { it.createdAt > sinceMillis },
        )
    }

    override fun observe(groupId: String): Flow<RemoteGroupContents> = contents.map {
        it[groupId]
            ?: EMPTY
    }

    override fun observeGroupsFor(userId: String): Flow<List<RemoteGroup>> =
        groups.map { all -> all.values.filter { userId in it.memberUserIds } }

    override suspend fun write(batch: RemoteWrite) {
        require(batch.size <= RemoteDataSource.MAX_BATCH_SIZE)
        beforeWrite?.also { beforeWrite = null }?.invoke()
        if (failWrites) error("Firestore unavailable")
        batches += batch
    }

    companion object {
        val EMPTY = RemoteGroupContents(emptyList(), emptyList(), emptyList(), emptyList())
    }
}
