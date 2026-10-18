package com.split.easy.sync.upload

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.split.easy.domain.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Runs an upload in the background; WorkManager retries with backoff if it fails (FR-10). */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val uploader: Uploader,
    private val auth: AuthRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Signed out: there's nobody to upload as; the next sign-in starts fresh anyway.
        if (auth.currentUser() == null) return Result.success()
        return try {
            uploader.upload()
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Upload failed, will retry", e)
            if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    private companion object {
        const val TAG = "SyncWorker"

        /** After this many tries the job stops; the next local change or app start starts it again. */
        const val MAX_ATTEMPTS = 10
    }
}
