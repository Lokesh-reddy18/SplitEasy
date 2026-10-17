package com.split.easy.data.repository

import com.split.easy.data.local.entity.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupRepositoryTest : RepositoryTest() {

    private val repository by lazy {
        OfflineFirstGroupRepository(db, currentUser, ids, movingClock, syncScheduler)
    }

    @Test
    fun `creating a group adds you as creator, then the guests, all waiting to sync`() = runTest {
        val groupId = repository.createGroup("  Coorg trip ", listOf("Priya", " ", "Meera "))

        val group = repository.observeGroup(groupId).first()!!
        assertEquals("Coorg trip", group.name)
        assertEquals("uid-you", group.createdByUserId)
        assertEquals(now, group.lastActivityAt)

        val members = db.memberDao().observeCurrentMembers(groupId).first()
        assertEquals(listOf("You", "Priya", "Meera"), members.map { it.displayName })
        assertEquals(listOf("uid-you", null, null), members.map { it.userId })
        assertTrue(members.all { it.syncState == SyncState.PENDING })
        assertEquals(1, syncRequests)
    }

    @Test
    fun `home lists groups with the most recent activity first`() = runTest {
        val flat = repository.createGroup("Flat 4B", emptyList())
        advanceClock()
        repository.createGroup("Coorg trip", emptyList())
        advanceClock()
        repository.renameGroup(flat, "Flat 4B (2026)")

        assertEquals(
            listOf("Flat 4B (2026)", "Coorg trip"),
            repository.observeActiveGroups().first().map { it.name },
        )
    }

    @Test
    fun `archiving moves the group to the archived list`() = runTest {
        val groupId = repository.createGroup("Goa trip", emptyList())
        repository.setArchived(groupId, isArchived = true)

        assertEquals(emptyList<Any>(), repository.observeActiveGroups().first())
        assertEquals(listOf(groupId), repository.observeArchivedGroups().first().map { it.id })

        repository.setArchived(groupId, isArchived = false)
        assertEquals(listOf(groupId), repository.observeActiveGroups().first().map { it.id })
    }
}
