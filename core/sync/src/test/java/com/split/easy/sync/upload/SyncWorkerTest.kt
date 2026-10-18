package com.split.easy.sync.upload

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.split.easy.data.local.DatabaseTest
import com.split.easy.data.local.group
import com.split.easy.domain.model.CurrentUser
import com.split.easy.sync.remote.FakeRemoteDataSource
import com.split.easy.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncWorkerTest : DatabaseTest() {

    private val remote = FakeRemoteDataSource()
    private val auth = FakeAuthRepository(CurrentUser("uid-you", "You"))

    private fun worker(runAttemptCount: Int = 0): SyncWorker {
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ) = SyncWorker(appContext, workerParameters, Uploader(db.syncDao(), remote), auth)
        }
        return TestListenableWorkerBuilder<SyncWorker>(ApplicationProvider.getApplicationContext())
            .setWorkerFactory(factory)
            .setRunAttemptCount(runAttemptCount)
            .build()
    }

    @Test
    fun `uploads and succeeds`() = runTest {
        db.groupDao().upsert(group("g1"))
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals(1, remote.batches.size)
    }

    @Test
    fun `a failed upload is retried, then given up after 10 tries`() = runTest {
        db.groupDao().upsert(group("g1"))
        remote.failWrites = true
        assertEquals(ListenableWorker.Result.retry(), worker(runAttemptCount = 3).doWork())
        assertEquals(ListenableWorker.Result.failure(), worker(runAttemptCount = 10).doWork())
    }

    @Test
    fun `nothing is uploaded while signed out`() = runTest {
        db.groupDao().upsert(group("g1"))
        auth.signOut()
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals(emptyList<Any>(), remote.batches)
    }
}
