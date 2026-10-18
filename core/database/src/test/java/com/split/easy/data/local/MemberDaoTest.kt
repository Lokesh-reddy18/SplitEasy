package com.split.easy.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MemberDaoTest : DatabaseTest() {

    private val dao get() = db.memberDao()

    @Test
    fun `members come back in join order, then id`() = runTest {
        dao.upsertAll(
            listOf(
                member("meera", createdAt = 3),
                member("you", createdAt = 1),
                member("ravi", createdAt = 3),
                member("priya", createdAt = 2),
            ),
        )
        assertEquals(
            listOf("you", "priya", "meera", "ravi"),
            dao.observeCurrentMembers("g1").first().map { it.id },
        )
    }

    @Test
    fun `removed members are hidden from new expenses but kept for history`() = runTest {
        dao.upsertAll(listOf(member("you", createdAt = 1), member("arjun", createdAt = 2)))
        dao.softDelete("arjun", updatedAt = 10)

        assertEquals(listOf("you"), dao.observeCurrentMembers("g1").first().map { it.id })
        assertEquals(listOf("you", "arjun"), dao.observeAllMembers("g1").first().map { it.id })
    }

    @Test
    fun `finds the user's member row in every group`() = runTest {
        dao.upsertAll(
            listOf(
                member("m1", groupId = "g1", userId = "uid-you"),
                member("m2", groupId = "g2", userId = "uid-you"),
                member("m3", groupId = "g3", userId = "uid-you", isDeleted = true),
                member("m4", groupId = "g1", userId = "uid-priya"),
            ),
        )
        assertEquals(
            setOf("m1", "m2"),
            dao.getMembershipsOfUser("uid-you").map { it.id }.toSet(),
        )
    }
}
