package com.split.easy.data.repository

import com.split.easy.data.local.entity.SyncState
import com.split.easy.domain.model.Category
import com.split.easy.domain.model.ExpenseDraft
import com.split.easy.domain.model.SplitType
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseRepositoryTest : RepositoryTest() {

    private val groups by lazy {
        OfflineFirstGroupRepository(db, currentUser, ids, movingClock, syncScheduler)
    }
    private val repository by lazy {
        OfflineFirstExpenseRepository(db, currentUser, ids, movingClock, syncScheduler)
    }

    private fun dinner(groupId: String) = ExpenseDraft(
        groupId = groupId,
        paidByMemberId = "priya",
        amountPaise = 2_40_000,
        description = " Dinner at Raintree ",
        category = Category.FOOD,
        date = LocalDate.of(2026, 10, 10),
        splitType = SplitType.EQUAL,
        sharesPaise = mapOf("you" to 1_20_000, "priya" to 1_20_000),
    )

    @Test
    fun `adding an expense saves it with its shares, by you, waiting to sync`() = runTest {
        val groupId = groups.createGroup("Coorg trip", emptyList())
        val expenseId = repository.addExpense(dinner(groupId))

        val expense = repository.observeExpense(expenseId).first()!!
        assertEquals("Dinner at Raintree", expense.description)
        assertEquals(mapOf("priya" to 1_20_000L, "you" to 1_20_000L), expense.sharesPaise)
        assertEquals("uid-you", expense.createdByUserId)
        assertFalse(expense.isSynced)
        assertEquals(2, syncRequests)
    }

    @Test
    fun `adding an expense moves the group's activity time but not its updatedAt`() = runTest {
        val groupId = groups.createGroup("Coorg trip", emptyList())
        val created = db.groupDao().getGroup(groupId)!!
        db.groupDao().upsert(created.copy(syncState = SyncState.SYNCED))

        advanceClock(60_000)
        repository.addExpense(dinner(groupId))

        val group = db.groupDao().getGroup(groupId)!!
        assertEquals(now, group.lastActivityAt)
        assertEquals(created.updatedAt, group.updatedAt)
        assertEquals(SyncState.SYNCED, group.syncState)
    }

    @Test
    fun `editing keeps who added it and when, and replaces the shares`() = runTest {
        val groupId = groups.createGroup("Coorg trip", emptyList())
        val expenseId = repository.addExpense(dinner(groupId))
        val addedAt = now

        advanceClock()
        val percentSplit = dinner(groupId).copy(
            splitType = SplitType.PERCENT,
            sharesPaise = mapOf("priya" to 80_000, "meera" to 1_60_000),
            percentBasisPoints = mapOf("priya" to 3_333, "meera" to 6_667),
        )
        repository.updateExpense(expenseId, percentSplit)

        val expense = repository.observeExpense(expenseId).first()!!
        assertEquals(addedAt, expense.createdAt)
        assertEquals("uid-you", expense.createdByUserId)
        assertEquals(SplitType.PERCENT, expense.splitType)
        assertEquals(mapOf("meera" to 1_60_000L, "priya" to 80_000L), expense.sharesPaise)
        assertEquals(mapOf("meera" to 6_667, "priya" to 3_333), expense.percentBasisPoints)
    }

    @Test
    fun `deleted expenses disappear from the feed`() = runTest {
        val groupId = groups.createGroup("Coorg trip", emptyList())
        val expenseId = repository.addExpense(dinner(groupId))
        repository.deleteExpense(expenseId)

        assertEquals(emptyList<Any>(), repository.observeExpenses(groupId).first())
        assertNull(repository.observeExpense(expenseId).first())
        assertEquals(SyncState.PENDING, db.expenseDao().getExpense(expenseId)!!.syncState)
    }
}
