package com.split.easy.data.repository

import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettlementRepositoryTest : RepositoryTest() {

    private val groups by lazy {
        OfflineFirstGroupRepository(db, currentUser, ids, movingClock, syncScheduler)
    }
    private val members by lazy {
        OfflineFirstMemberRepository(db, currentUser, ids, movingClock, syncScheduler)
    }
    private val repository by lazy {
        OfflineFirstSettlementRepository(db, currentUser, ids, movingClock, syncScheduler)
    }

    @Test
    fun `a UPI payment is saved as pending with its transaction id and today's date`() = runTest {
        val groupId = groups.createGroup("Coorg trip", listOf("Ravi"))
        val (me, ravi) = members.observeMembers(groupId).first()

        repository.recordSettlement(
            groupId,
            fromMemberId = ravi.id,
            toMemberId = me.id,
            amountPaise = 1_20_000,
            method = SettlementMethod.UPI,
            status = SettlementStatus.PENDING,
            upiTxnId = "481022756631",
        )

        val saved = repository.observeSettlements(groupId).first().single()
        assertEquals(SettlementStatus.PENDING, saved.status)
        assertEquals("481022756631", saved.upiTxnId)
        assertEquals(LocalDate.of(2026, 10, 10), saved.date)
    }

    @Test
    fun `badge counts payments waiting for me, and confirming clears it`() = runTest {
        val groupId = groups.createGroup("Coorg trip", listOf("Ravi"))
        val (me, ravi) = members.observeMembers(groupId).first()
        val id = repository.recordSettlement(
            groupId,
            ravi.id,
            me.id,
            1_20_000,
            SettlementMethod.CASH,
            SettlementStatus.PENDING,
        )
        assertEquals(mapOf(groupId to 1), repository.observeWaitingForMyConfirmation().first())

        repository.setStatus(id, SettlementStatus.CONFIRMED)
        assertEquals(emptyMap<String, Int>(), repository.observeWaitingForMyConfirmation().first())
        assertEquals(
            SettlementStatus.CONFIRMED,
            repository.observeSettlements(groupId).first().single().status,
        )
    }

    @Test
    fun `cancelling a settlement removes it`() = runTest {
        val groupId = groups.createGroup("Coorg trip", listOf("Ravi"))
        val (me, ravi) = members.observeMembers(groupId).first()
        val id = repository.recordSettlement(
            groupId,
            me.id,
            ravi.id,
            500,
            SettlementMethod.CASH,
            SettlementStatus.CONFIRMED,
        )
        repository.deleteSettlement(id)
        assertEquals(emptyList<Any>(), repository.observeSettlements(groupId).first())
    }
}
