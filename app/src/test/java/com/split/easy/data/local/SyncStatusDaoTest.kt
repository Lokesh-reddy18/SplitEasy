package com.split.easy.data.local

import app.cash.turbine.test
import com.split.easy.data.local.entity.SyncState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncStatusDaoTest : DatabaseTest() {

    @Test
    fun `counts pending changes across all tables and updates when they sync`() = runTest {
        db.syncStatusDao().observePendingChangeCount().test {
            assertEquals(0, awaitItem())

            db.groupDao().upsert(group("g1"))
            assertEquals(1, awaitItem())

            db.memberDao().upsert(member("you"))
            assertEquals(2, awaitItem())

            db.expenseDao().saveWithShares(expense("cab"), emptyList())
            assertEquals(3, awaitItem())

            db.settlementDao().upsert(settlement("s1", from = "ravi", to = "you"))
            assertEquals(4, awaitItem())

            // The sync worker (M4) will mark records as SYNCED after uploading them.
            db.groupDao().upsert(group("g1", syncState = SyncState.SYNCED))
            assertEquals(3, awaitItem())
        }
    }
}
