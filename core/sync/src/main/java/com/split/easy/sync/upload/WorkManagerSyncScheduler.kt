package com.split.easy.sync.upload

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.split.easy.domain.sync.SyncScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Queues an upload as unique work that waits for a network connection (FR-10).
 *
 * APPEND_OR_REPLACE means a change made while an upload is running queues one more upload after
 * it, so nothing is left behind; many quick changes still collapse into at most one waiting job.
 */
class WorkManagerSyncScheduler @Inject constructor(private val workManager: WorkManager) :
    SyncScheduler {

    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    companion object {
        const val WORK_NAME = "sync"
        private const val BACKOFF_SECONDS = 10L
    }
}
