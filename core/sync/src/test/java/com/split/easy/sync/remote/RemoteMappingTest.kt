package com.split.easy.sync.remote

import com.split.easy.data.local.entity.ExpenseEntity
import com.split.easy.data.local.entity.ExpenseShareEntity
import com.split.easy.data.local.entity.GroupEntity
import com.split.easy.data.local.entity.GroupEventEntity
import com.split.easy.data.local.entity.MemberEntity
import com.split.easy.data.local.entity.SettlementEntity
import com.split.easy.data.local.entity.SyncState
import com.split.easy.domain.model.Category
import com.split.easy.domain.model.GroupEventType
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.model.SplitType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteMappingTest {

    private val date = LocalDate.of(2026, 10, 10)

    @Test
    fun `a group round-trips, gains its member uids, and arrives as synced`() {
        val group =
            GroupEntity(
                "g1",
                "Coorg trip",
                "uid-a",
                isArchived = true,
                lastActivityAt = 5,
                updatedAt = 4,
            )
        val remote = group.toRemote(listOf("uid-b", "uid-a", "uid-b"))
        assertEquals(listOf("uid-a", "uid-b"), remote.memberUserIds)
        assertEquals(group.copy(syncState = SyncState.SYNCED), remote.toEntity())
    }

    @Test
    fun `members round-trip, guests included`() {
        val guest =
            MemberEntity(
                "m1",
                "g1",
                "Arjun",
                userId = null,
                upiId = "arjun@oksbi",
                createdAt = 1,
                updatedAt = 2,
            )
        val removed =
            MemberEntity(
                "m2",
                "g1",
                "Ravi",
                userId = "uid-r",
                createdAt = 3,
                updatedAt = 4,
                isDeleted = true,
            )
        listOf(guest, removed).forEach {
            assertEquals(it.copy(syncState = SyncState.SYNCED), it.toRemote().toEntity())
        }
    }

    @Test
    fun `an expense keeps its shares and typed percentages`() {
        val expense = ExpenseEntity(
            id = "e1",
            groupId = "g1",
            paidBy = "m1",
            amountPaise = 10_000,
            description = "Snacks",
            category = Category.FOOD,
            date = date,
            splitType = SplitType.PERCENT,
            createdBy = "uid-a",
            createdAt = 1,
            updatedAt = 2,
        )
        val shares = listOf(
            ExpenseShareEntity("e1", "m1", 3_333, 3_333),
            ExpenseShareEntity("e1", "m2", 6_667, 6_667),
        )
        val remote = expense.toRemote(shares)
        assertEquals("FOOD", remote.category)
        assertEquals("2026-10-10", remote.date)
        assertEquals(RemoteShare(3_333, 3_333), remote.shares["m1"])

        val (back, backShares) = remote.toEntities()
        assertEquals(expense.copy(syncState = SyncState.SYNCED), back)
        assertEquals(shares, backShares)
    }

    @Test
    fun `values from a newer app version fall back safely`() {
        val local = ExpenseEntity(
            "e1", "g1", "m1", 100, "x", Category.FOOD, date, SplitType.EQUAL, "u", 1, 2,
        )
        val expense = local.toRemote(emptyList()).copy(category = "PETS", splitType = "SHARES")
        val (entity) = expense.toEntities()
        assertEquals(Category.OTHER, entity.category)
        assertEquals(SplitType.EXACT, entity.splitType)

        val confirmed = SettlementEntity(
            "s1", "g1", "a", "b", 100, date, null,
            SettlementStatus.CONFIRMED, SettlementMethod.UPI, 1,
        )
        val settlement = confirmed.toRemote().copy(status = "DISPUTED", method = "CARD")
        assertEquals(SettlementStatus.PENDING, settlement.toEntity().status)
        assertEquals(SettlementMethod.CASH, settlement.toEntity().method)
    }

    @Test
    fun `settlements and events round-trip`() {
        val settlement = SettlementEntity(
            "s1", "g1", "ravi", "you", 1_20_000, date, "481022756631",
            SettlementStatus.PENDING, SettlementMethod.UPI, updatedAt = 9,
        )
        assertEquals(
            settlement.copy(syncState = SyncState.SYNCED),
            settlement.toRemote().toEntity(),
        )

        val event =
            GroupEventEntity(
                "ev1",
                "g1",
                "priya",
                "arjun",
                GroupEventType.GUEST_UPI_CHANGED,
                null,
                "arjun@oksbi",
                7,
            )
        assertEquals(event.copy(syncState = SyncState.SYNCED), event.toRemote().toEntity())
    }
}
