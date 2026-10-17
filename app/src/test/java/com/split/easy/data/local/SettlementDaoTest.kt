package com.split.easy.data.local

import com.split.easy.data.local.dao.GroupCount
import com.split.easy.domain.model.SettlementStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettlementDaoTest : DatabaseTest() {

    private val dao get() = db.settlementDao()

    @Test
    fun `badge counts pending payments waiting for this user, per group`() = runTest {
        db.memberDao().upsertAll(
            listOf(
                member("you-g1", groupId = "g1", userId = "uid-you"),
                member("you-g2", groupId = "g2", userId = "uid-you"),
                member("priya-g1", groupId = "g1", userId = "uid-priya"),
                member("ravi-g1", groupId = "g1"),
            ),
        )
        dao.upsert(settlement("s1", from = "ravi-g1", to = "you-g1"))
        dao.upsert(settlement("s2", from = "priya-g1", to = "you-g1"))
        dao.upsert(settlement("s3", from = "x", to = "you-g2", groupId = "g2"))
        // These don't count: confirmed, cancelled, rejected, or for someone else.
        dao.upsert(
            settlement("s4", from = "ravi-g1", to = "you-g1", status = SettlementStatus.CONFIRMED),
        )
        dao.upsert(settlement("s5", from = "ravi-g1", to = "you-g1", isDeleted = true))
        dao.upsert(
            settlement("s6", from = "ravi-g1", to = "you-g1", status = SettlementStatus.REJECTED),
        )
        dao.upsert(settlement("s7", from = "you-g1", to = "priya-g1"))

        assertEquals(
            setOf(GroupCount("g1", 2), GroupCount("g2", 1)),
            dao.observeWaitingForConfirmation("uid-you").first().toSet(),
        )
        assertEquals(
            listOf(GroupCount("g1", 1)),
            dao.observeWaitingForConfirmation("uid-priya").first(),
        )
    }

    @Test
    fun `confirming changes the status and marks it for sync`() = runTest {
        dao.upsert(settlement("s1", from = "ravi", to = "you"))
        dao.updateStatus("s1", SettlementStatus.CONFIRMED, updatedAt = 3)

        val saved = dao.observeSettlements("g1").first().single()
        assertEquals(SettlementStatus.CONFIRMED, saved.status)
        assertEquals(3L, saved.updatedAt)
    }

    @Test
    fun `cancelled settlements disappear`() = runTest {
        dao.upsert(settlement("s1", from = "ravi", to = "you"))
        dao.softDelete("s1", updatedAt = 3)
        assertEquals(emptyList<Any>(), dao.observeSettlements("g1").first())
    }
}
