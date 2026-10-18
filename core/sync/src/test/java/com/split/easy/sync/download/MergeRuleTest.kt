package com.split.easy.sync.download

import com.split.easy.data.local.entity.SyncState.PENDING
import com.split.easy.data.local.entity.SyncState.SYNCED
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeRuleTest {

    @Test
    fun `new records are always taken`() {
        assertTrue(shouldTakeRemote(localUpdatedAt = null, localState = null, remoteUpdatedAt = 1))
    }

    @Test
    fun `an uploaded record takes newer or equal remote versions`() {
        assertTrue(shouldTakeRemote(10, SYNCED, 11))
        assertTrue(shouldTakeRemote(10, SYNCED, 10)) // our own edit coming back
        assertFalse(shouldTakeRemote(10, SYNCED, 9))
    }

    @Test
    fun `an edit not yet uploaded wins unless the remote one is strictly newer`() {
        assertFalse(shouldTakeRemote(10, PENDING, 9))
        assertFalse(shouldTakeRemote(10, PENDING, 10))
        assertTrue(shouldTakeRemote(10, PENDING, 11))
    }
}
