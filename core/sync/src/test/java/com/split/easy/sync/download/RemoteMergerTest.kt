package com.split.easy.sync.download

import com.split.easy.data.local.DatabaseTest
import com.split.easy.data.local.entity.ExpenseShareEntity
import com.split.easy.data.local.entity.SyncState
import com.split.easy.data.local.expense
import com.split.easy.data.local.group
import com.split.easy.data.local.member
import com.split.easy.sync.remote.RemoteEvent
import com.split.easy.sync.remote.RemoteGroupContents
import com.split.easy.sync.remote.RemoteShare
import com.split.easy.sync.remote.toRemote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteMergerTest : DatabaseTest() {

    private lateinit var merger: RemoteMerger

    @Before
    fun setUp() {
        merger = RemoteMerger(db, db.mergeDao())
    }

    private fun contents(
        members: List<com.split.easy.sync.remote.RemoteMember> = emptyList(),
        expenses: List<com.split.easy.sync.remote.RemoteExpense> = emptyList(),
        settlements: List<com.split.easy.sync.remote.RemoteSettlement> = emptyList(),
        events: List<RemoteEvent> = emptyList(),
    ) = RemoteGroupContents(members, expenses, settlements, events)

    @Test
    fun `a group from another device appears, already synced`() = runTest {
        val remote = group("g1", name = "Coorg trip").toRemote(listOf("uid-you"))
        assertEquals(1, merger.mergeGroups(listOf(remote)))
        val stored = db.groupDao().getGroup("g1")!!
        assertEquals("Coorg trip", stored.name)
        assertEquals(SyncState.SYNCED, stored.syncState)
        assertEquals(0, db.syncStatusDao().observePendingChangeCount().first())
    }

    @Test
    fun `a rename made here and not yet uploaded beats an older remote one`() = runTest {
        db.groupDao().upsert(group("g1", name = "Mine").copy(updatedAt = 20, lastActivityAt = 5))
        val older = group(
            "g1",
            name = "Theirs",
        ).copy(updatedAt = 10, lastActivityAt = 30).toRemote(emptyList())

        assertEquals(0, merger.mergeGroups(listOf(older)))
        val stored = db.groupDao().getGroup("g1")!!
        assertEquals("Mine", stored.name)
        assertEquals(SyncState.PENDING, stored.syncState)
        // ...but the newer activity still moves the group up Groups home.
        assertEquals(30, stored.lastActivityAt)
    }

    @Test
    fun `a newer remote edit replaces a pending local one`() = runTest {
        db.groupDao().upsert(group("g1", name = "Mine").copy(updatedAt = 10))
        merger.mergeGroups(
            listOf(group("g1", name = "Theirs").copy(updatedAt = 20).toRemote(emptyList())),
        )
        assertEquals("Theirs", db.groupDao().getGroup("g1")!!.name)
    }

    @Test
    fun `expenses arrive with their shares, edits replace shares, deletes arrive as deletes`() =
        runTest {
            db.groupDao().upsert(group("g1", syncState = SyncState.SYNCED))
            val shares =
                listOf(ExpenseShareEntity("e1", "you", 50), ExpenseShareEntity("e1", "arjun", 50))
            val remote = expense("e1").toRemote(shares)
            merger.mergeContents("g1", contents(expenses = listOf(remote)))
            assertEquals(2, db.syncDao().shares("e1").size)

            val edited = remote.copy(
                amountPaise = 100,
                updatedAt = remote.updatedAt + 1,
                shares = mapOf("you" to RemoteShare(100, null)),
            )
            merger.mergeContents("g1", contents(expenses = listOf(edited)))
            assertEquals(listOf("you"), db.syncDao().shares("e1").map { it.memberId })

            val deleted = edited.copy(isDeleted = true, updatedAt = edited.updatedAt + 1)
            merger.mergeContents("g1", contents(expenses = listOf(deleted)))
            assertTrue(db.expenseDao().getExpense("e1")!!.isDeleted)
            assertEquals(emptyList<Any>(), db.expenseDao().observeWithShares("g1").first())
        }

    @Test
    fun `members merge, and events are added once and never changed`() = runTest {
        db.groupDao().upsert(group("g1", syncState = SyncState.SYNCED))
        val arjun = member("arjun").copy(upiId = "arjun@oksbi", updatedAt = 5).toRemote()
        val event =
            RemoteEvent("ev1", "g1", "you", "arjun", "GUEST_UPI_CHANGED", null, "arjun@oksbi", 5)

        merger.mergeContents("g1", contents(members = listOf(arjun), events = listOf(event)))
        merger.mergeContents("g1", contents(events = listOf(event.copy(newValue = "changed@ybl"))))

        assertEquals("arjun@oksbi", db.memberDao().getMember("arjun")!!.upiId)
        assertEquals(
            listOf("arjun@oksbi"),
            db.groupEventDao().observeEvents("g1").first().map {
                it.newValue
            },
        )
    }

    @Test
    fun `new expenses from other devices move the group up Groups home`() = runTest {
        db.groupDao().upsert(group("g1", syncState = SyncState.SYNCED).copy(lastActivityAt = 1))
        val remote = expense("e1").toRemote(emptyList()).copy(updatedAt = 500)
        merger.mergeContents("g1", contents(expenses = listOf(remote)))
        assertEquals(500, db.groupDao().getGroup("g1")!!.lastActivityAt)
    }
}
