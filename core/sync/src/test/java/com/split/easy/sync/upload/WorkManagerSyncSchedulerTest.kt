package com.split.easy.sync.upload

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkManagerSyncSchedulerTest {

    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
    }

    private fun queued(): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(WorkManagerSyncScheduler.WORK_NAME).get()

    @Test
    fun `an upload waits for the network`() {
        WorkManagerSyncScheduler(workManager).requestSync()
        val work = queued().single()
        assertEquals(WorkInfo.State.ENQUEUED, work.state)
        assertEquals(NetworkType.CONNECTED, work.constraints.requiredNetworkType)
    }

    @Test
    fun `many quick changes don't pile up uploads`() {
        val scheduler = WorkManagerSyncScheduler(workManager)
        repeat(20) { scheduler.requestSync() }
        // While offline, each request replaces the waiting one, so only one upload stays queued.
        assertTrue(queued().count { it.state == WorkInfo.State.ENQUEUED } <= 1)
    }
}
