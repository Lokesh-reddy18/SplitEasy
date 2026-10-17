package com.split.easy.data.repository

import com.split.easy.domain.model.GroupEventType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemberRepositoryTest : RepositoryTest() {

    private val groups by lazy {
        OfflineFirstGroupRepository(db, currentUser, ids, movingClock, syncScheduler)
    }
    private val repository by lazy {
        OfflineFirstMemberRepository(db, currentUser, ids, movingClock, syncScheduler)
    }

    private suspend fun membersOf(groupId: String) = repository.observeMembers(groupId).first()

    @Test
    fun `added guests join at the end and removed members stay in history`() = runTest {
        val groupId = groups.createGroup("Coorg trip", listOf("Priya"))
        advanceClock()
        val ravi = repository.addGuest(groupId, " Ravi ")
        repository.removeMember(ravi)

        val members = membersOf(groupId)
        assertEquals(listOf("You", "Priya", "Ravi"), members.map { it.displayName })
        assertEquals(listOf(false, false, true), members.map { it.isRemoved })
    }

    @Test
    fun `setting a guest's UPI ID records who changed it, old and new`() = runTest {
        val groupId = groups.createGroup("Coorg trip", listOf("Arjun"))
        val (me, arjun) = membersOf(groupId)

        advanceClock()
        repository.setGuestUpiId(arjun.id, actorMemberId = me.id, upiId = "arjun@okaxis")
        advanceClock()
        repository.setGuestUpiId(arjun.id, actorMemberId = me.id, upiId = " arjun@oksbi ")

        assertEquals("arjun@oksbi", membersOf(groupId)[1].upiId)
        val events = repository.observeEvents(groupId).first()
        assertEquals(2, events.size)
        val (latest, first) = events
        assertNull(first.oldValue) // first time: "You set Arjun's UPI ID to arjun@okaxis" (H5)
        assertEquals("arjun@okaxis", first.newValue)
        assertEquals("arjun@okaxis", latest.oldValue)
        assertEquals("arjun@oksbi", latest.newValue)
        assertTrue(
            events.all {
                it.type == GroupEventType.GUEST_UPI_CHANGED &&
                    it.actorMemberId == me.id
            },
        )
    }

    @Test
    fun `setting the same UPI ID again adds no event`() = runTest {
        val groupId = groups.createGroup("Coorg trip", listOf("Arjun"))
        val (me, arjun) = membersOf(groupId)
        repository.setGuestUpiId(arjun.id, me.id, "arjun@oksbi")
        repository.setGuestUpiId(arjun.id, me.id, "arjun@oksbi")
        assertEquals(1, repository.observeEvents(groupId).first().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a real user's UPI ID can't be set by someone else`() = runTest {
        val groupId = groups.createGroup("Coorg trip", emptyList())
        val me = membersOf(groupId).single()
        repository.setGuestUpiId(me.id, actorMemberId = "someone-else", upiId = "x@ybl")
    }

    @Test
    fun `my UPI ID is set in every group I'm in`() = runTest {
        val coorg = groups.createGroup("Coorg trip", listOf("Priya"))
        val flat = groups.createGroup("Flat 4B", emptyList())

        repository.setMyUpiId("ananya.r@okaxis")

        assertEquals(listOf("ananya.r@okaxis", null), membersOf(coorg).map { it.upiId })
        assertEquals(listOf("ananya.r@okaxis"), membersOf(flat).map { it.upiId })
    }
}
