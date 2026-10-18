package com.split.easy.sync.download

import com.split.easy.data.local.DatabaseTest
import com.split.easy.data.local.entity.SyncState
import com.split.easy.data.local.expense
import com.split.easy.data.local.group
import com.split.easy.data.local.member
import com.split.easy.sync.remote.FakeRemoteDataSource
import com.split.easy.sync.remote.RemoteGroupContents
import com.split.easy.sync.remote.toRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DownloaderTest : DatabaseTest() {

    private val remote = FakeRemoteDataSource()
    private lateinit var downloader: Downloader

    @Before
    fun setUp() {
        val mergeDao = db.mergeDao()
        downloader = Downloader(remote, RemoteMerger(db, mergeDao), SyncProgressStore(mergeDao))
    }

    private val coorg = group(
        "g1",
        name = "Coorg trip",
        syncState = SyncState.SYNCED,
    ).toRemote(listOf("uid-you"))
    private val someoneElses = group("g2", name = "Not mine").toRemote(listOf("uid-other"))

    private fun groupContents(vararg expenses: com.split.easy.sync.remote.RemoteExpense) =
        RemoteGroupContents(
            members = listOf(member("you", userId = "uid-you").toRemote()),
            expenses = expenses.toList(),
            settlements = emptyList(),
            events = emptyList(),
        )

    @Test
    fun `catching up brings in only this user's groups and their contents`() = runTest {
        remote.groups.value = mapOf("g1" to coorg, "g2" to someoneElses)
        remote.contents.value = mapOf("g1" to groupContents(expense("e1").toRemote(emptyList())))

        downloader.catchUp("uid-you")

        assertEquals("Coorg trip", db.groupDao().getGroup("g1")!!.name)
        assertNull(db.groupDao().getGroup("g2"))
        assertEquals(1, db.expenseDao().observeWithShares("g1").first().size)
        assertEquals(0, db.syncStatusDao().observePendingChangeCount().first())
    }

    @Test
    fun `the next catch-up asks only for newer changes`() = runTest {
        val old = expense("e1").toRemote(emptyList()).copy(updatedAt = 10 * 60_000L)
        remote.groups.value = mapOf("g1" to coorg)
        remote.contents.value = mapOf("g1" to groupContents(old))
        downloader.catchUp("uid-you")
        assertEquals(10 * 60_000L, db.mergeDao().lastDownloadedAt("g1"))

        // A second expense, 30 minutes later; the first one isn't fetched again.
        val new = expense("e2").toRemote(emptyList()).copy(updatedAt = 40 * 60_000L)
        remote.contents.value = mapOf("g1" to groupContents(old, new))
        downloader.catchUp("uid-you")
        assertEquals(2, db.expenseDao().observeWithShares("g1").first().size)
    }

    @Test
    fun `live listening picks up changes while the app is open and skips archived groups`() =
        runTest {
            val archived = group(
                "g3",
                name = "Old trip",
                isArchived = true,
            ).toRemote(listOf("uid-you"))
            remote.groups.value = mapOf("g1" to coorg, "g3" to archived)
            remote.contents.value =
                mapOf(
                    "g1" to groupContents(),
                    "g3" to groupContents(expense("x", groupId = "g3").toRemote(emptyList())),
                )

            val listening =
                launch(UnconfinedTestDispatcher(testScheduler)) { downloader.listen("uid-you") }
            // Room writes on its own threads, so wait (briefly) for the listener's results.
            val groups = db.groupDao().observeArchivedGroups().waitFor { it.isNotEmpty() }
            assertEquals("Old trip", groups.single().name)
            assertEquals(0, db.expenseDao().observeWithShares("g3").first().size)

            remote.contents.value =
                mapOf("g1" to groupContents(expense("e9").toRemote(emptyList())))
            val expenses = db.expenseDao().observeWithShares("g1").waitFor { it.isNotEmpty() }
            assertEquals(listOf("e9"), expenses.map { it.expense.id })
            listening.cancel()
        }

    private suspend fun <T> Flow<T>.waitFor(condition: (T) -> Boolean): T =
        withContext(Dispatchers.Default) { withTimeout(5_000) { first(condition) } }
}
