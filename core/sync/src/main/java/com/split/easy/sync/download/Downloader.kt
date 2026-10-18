package com.split.easy.sync.download

import android.util.Log
import com.split.easy.sync.remote.RemoteDataSource
import com.split.easy.sync.remote.RemoteGroupContents
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Brings other devices' changes into Room (M4-Q8).
 *
 * - [catchUp] runs when the app opens: it fetches this user's groups, then, for each group, only
 *   what changed since that group was last downloaded.
 * - [listen] runs while the app is in the foreground: live listeners on the user's groups and on
 *   every active (not archived) group, so changes appear within seconds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Downloader @Inject constructor(
    private val remote: RemoteDataSource,
    private val merger: RemoteMerger,
    private val progress: SyncProgressStore,
) {
    /** Returns how many records changed on this phone. Throws if Firestore can't be reached. */
    suspend fun catchUp(userId: String): Int {
        val groups = remote.groupsFor(userId)
        var changed = merger.mergeGroups(groups)
        for (group in groups) {
            val since = progress.lastDownloaded(group.id)
            val contents = remote.changesIn(group.id, since)
            changed += merger.mergeContents(group.id, contents)
            progress.setLastDownloaded(group.id, contents.newestTime())
        }
        return changed
    }

    /** Never completes on its own; cancel the collecting coroutine to stop listening. */
    suspend fun listen(userId: String) = coroutineScope {
        launch {
            remote.observeGroupsFor(userId).retrying().collect { merger.mergeGroups(it) }
        }
        remote.observeGroupsFor(userId)
            .retrying()
            .map { groups ->
                groups.filter { !it.isArchived && !it.isDeleted }.map { it.id }.toSet()
            }
            .distinctUntilChanged()
            .flatMapLatest { ids -> merge(*ids.map { id -> listenToGroup(id) }.toTypedArray()) }
            .collect()
    }

    private fun listenToGroup(groupId: String): Flow<Unit> = remote.observe(groupId)
        .retrying()
        .onEach { contents ->
            merger.mergeContents(groupId, contents)
            progress.setLastDownloaded(groupId, contents.newestTime())
        }
        .map { }

    /** A listener that fails (offline, or permission briefly missing) is logged and stays quiet. */
    private fun <T> Flow<T>.retrying(): Flow<T> = catch { Log.w(TAG, "Listener stopped", it) }

    private fun RemoteGroupContents.newestTime(): Long = maxOf(
        members.maxOfOrNull { it.updatedAt } ?: 0,
        expenses.maxOfOrNull { it.updatedAt } ?: 0,
        settlements.maxOfOrNull { it.updatedAt } ?: 0,
        events.maxOfOrNull { it.createdAt } ?: 0,
    )

    private companion object {
        const val TAG = "Downloader"
    }
}
