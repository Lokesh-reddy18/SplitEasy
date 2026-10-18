package com.split.easy.data.local

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupDaoTest : DatabaseTest() {

    private val dao get() = db.groupDao()

    @Test
    fun `home list hides archived and deleted groups`() = runTest {
        dao.upsert(group("g1", name = "Flat 4B"))
        dao.upsert(group("g2", name = "Coorg trip"))
        dao.upsert(group("g3", name = "Old trip", isArchived = true))
        dao.upsert(group("g4", name = "Deleted", isDeleted = true))

        assertEquals(
            listOf("Coorg trip", "Flat 4B"),
            dao.observeActiveGroups().first().map {
                it.name
            },
        )
        assertEquals(listOf("Old trip"), dao.observeArchivedGroups().first().map { it.name })
    }

    @Test
    fun `archiving moves a group to the archived list and marks it for sync`() = runTest {
        dao.upsert(group("g1", syncState = com.split.easy.data.local.entity.SyncState.SYNCED))

        dao.observeActiveGroups().test {
            assertEquals(1, awaitItem().size)
            dao.setArchived("g1", isArchived = true, updatedAt = 5L)
            assertEquals(0, awaitItem().size)
        }
        val archived = dao.observeArchivedGroups().first().single()
        assertEquals(5L, archived.updatedAt)
        assertEquals(com.split.easy.data.local.entity.SyncState.PENDING, archived.syncState)
    }

    @Test
    fun `soft-deleted group disappears`() = runTest {
        dao.upsert(group("g1"))
        dao.softDelete("g1", updatedAt = 9L)
        assertNull(dao.observeGroup("g1").first())
    }
}
