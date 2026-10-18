package com.split.easy.sync.upload

import com.split.easy.data.local.DatabaseTest
import com.split.easy.data.local.entity.ExpenseShareEntity
import com.split.easy.data.local.entity.GroupEventEntity
import com.split.easy.data.local.entity.SyncState
import com.split.easy.data.local.expense
import com.split.easy.data.local.group
import com.split.easy.data.local.member
import com.split.easy.data.local.settlement
import com.split.easy.domain.model.GroupEventType
import com.split.easy.sync.remote.FakeRemoteDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UploaderTest : DatabaseTest() {

    private val remote = FakeRemoteDataSource()
    private lateinit var uploader: Uploader

    @Before
    fun setUp() {
        uploader = Uploader(db.syncDao(), remote)
    }

    private suspend fun pendingCount() = db.syncStatusDao().observePendingChangeCount().first()

    @Test
    fun `uploads every pending record once and marks it synced`() = runTest {
        db.groupDao().upsert(group("g1"))
        db.memberDao().upsertAll(listOf(member("you", userId = "uid-you"), member("arjun")))
        db.expenseDao().saveWithShares(
            expense("e1"),
            listOf(ExpenseShareEntity("e1", "you", 100), ExpenseShareEntity("e1", "arjun", 100)),
        )
        db.settlementDao().upsert(settlement("s1", from = "arjun", to = "you"))
        assertEquals(5, pendingCount())

        assertEquals(5, uploader.upload())

        val batch = remote.batches.single()
        assertEquals(listOf("g1"), batch.groups.map { it.id })
        assertEquals(listOf("uid-you"), batch.groups.single().memberUserIds)
        assertEquals(setOf("you", "arjun"), batch.members.map { it.id }.toSet())
        assertEquals(setOf("you", "arjun"), batch.expenses.single().shares.keys)
        assertEquals(listOf("s1"), batch.settlements.map { it.id })
        assertEquals(0, pendingCount())

        // Nothing left to send.
        assertEquals(0, uploader.upload())
        assertEquals(1, remote.batches.size)
    }

    @Test
    fun `already synced records aren't uploaded`() = runTest {
        db.groupDao().upsert(group("g1", syncState = SyncState.SYNCED))
        assertEquals(0, uploader.upload())
        assertTrue(remote.batches.isEmpty())
    }

    @Test
    fun `an edit made during the upload stays pending and goes up next time`() = runTest {
        db.groupDao().upsert(group("g1", name = "Coorg"))
        remote.beforeWrite = { db.groupDao().rename("g1", "Coorg trip", updatedAt = 99) }

        uploader.upload()
        assertEquals("Coorg", remote.batches.single().groups.single().name)
        assertEquals(1, pendingCount())

        uploader.upload()
        assertEquals("Coorg trip", remote.batches.last().groups.single().name)
        assertEquals(0, pendingCount())
    }

    @Test
    fun `a failed write leaves everything pending`() = runTest {
        db.groupDao().upsert(group("g1"))
        remote.failWrites = true
        runCatching { uploader.upload() }
        assertEquals(1, pendingCount())
    }

    @Test
    fun `a guest's UPI ID change goes in the same batch as its event`() = runTest {
        db.groupDao().upsert(group("g1", syncState = SyncState.SYNCED))
        db.memberDao().upsert(member("arjun").copy(upiId = "arjun@oksbi", updatedAt = 5))
        db.groupEventDao().insert(
            GroupEventEntity(
                "ev1",
                "g1",
                "you",
                "arjun",
                GroupEventType.GUEST_UPI_CHANGED,
                null,
                "arjun@oksbi",
                5,
            ),
        )

        uploader.upload()
        val batch = remote.batches.single()
        assertEquals(listOf("arjun"), batch.members.map { it.id })
        assertEquals(listOf("ev1"), batch.events.map { it.id })
        assertEquals(0, pendingCount())
    }

    @Test
    fun `large uploads are split into batches of at most 500`() = runTest {
        db.groupDao().upsert(group("g1"))
        db.memberDao().upsertAll((1..1_200).map { member("m$it", createdAt = it.toLong()) })

        assertEquals(1_201, uploader.upload())
        assertEquals(listOf(500, 500, 201), remote.batches.map { it.size })
        assertEquals(0, pendingCount())
    }
}
