package com.split.easy.sync.download

import android.util.Log
import com.split.easy.domain.sync.SyncScheduler
import javax.inject.Inject

/**
 * What runs while the app is on screen: upload anything pending, catch up with Firestore, then
 * listen live until the app goes to the background (the caller cancels it then).
 */
class ForegroundSync @Inject constructor(
    private val downloader: Downloader,
    private val syncScheduler: SyncScheduler,
) {
    suspend fun run(userId: String) {
        syncScheduler.requestSync()
        try {
            downloader.catchUp(userId)
        } catch (e: Exception) {
            // Offline or Firestore unavailable: the live listeners below pick up once reconnected.
            Log.w(TAG, "Catch-up failed", e)
        }
        downloader.listen(userId)
    }

    private companion object {
        const val TAG = "ForegroundSync"
    }
}
