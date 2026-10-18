package com.split.easy.sync.download

import com.split.easy.data.local.dao.MergeDao
import com.split.easy.data.local.entity.SyncProgressEntity
import javax.inject.Inject

/** Per-group download progress (see [SyncProgressEntity]). */
class SyncProgressStore @Inject constructor(private val mergeDao: MergeDao) {

    /**
     * Where the next catch-up starts. It steps back a few minutes so a change saved on another phone
     * whose clock is slightly behind isn't missed; merging the same change twice is harmless.
     */
    suspend fun lastDownloaded(groupId: String): Long =
        mergeDao.lastDownloadedAt(groupId)?.let { (it - CLOCK_MARGIN_MILLIS).coerceAtLeast(0) } ?: 0

    suspend fun setLastDownloaded(groupId: String, at: Long) {
        val current = mergeDao.lastDownloadedAt(groupId) ?: 0
        if (at > current) mergeDao.setProgress(SyncProgressEntity(groupId, at))
    }

    private companion object {
        const val CLOCK_MARGIN_MILLIS = 5 * 60 * 1000L
    }
}
