package com.split.easy.data.local

import com.split.easy.data.local.entity.GroupEventEntity
import com.split.easy.domain.model.GroupEventType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupEventDaoTest : DatabaseTest() {

    private val dao get() = db.groupEventDao()

    private fun upiChange(id: String, old: String?, new: String, createdAt: Long) =
        GroupEventEntity(
            id = id,
            groupId = "g1",
            actorMemberId = "arjun",
            targetMemberId = "ravi",
            type = GroupEventType.GUEST_UPI_CHANGED,
            oldValue = old,
            newValue = new,
            createdAt = createdAt,
        )

    @Test
    fun `events come back newest first`() = runTest {
        dao.insert(upiChange("e1", old = null, new = "ravi@okhdfc", createdAt = 1))
        dao.insert(upiChange("e2", old = "ravi@okhdfc", new = "arjun@okaxis", createdAt = 2))

        assertEquals(listOf("e2", "e1"), dao.observeEvents("g1").first().map { it.id })
    }

    @Test
    fun `inserting an existing event never overwrites it`() = runTest {
        val original = upiChange("e1", old = null, new = "ravi@okhdfc", createdAt = 1)
        dao.insert(original)
        dao.insert(original.copy(newValue = "someone@else"))

        assertEquals(listOf(original), dao.observeEvents("g1").first())
    }
}
