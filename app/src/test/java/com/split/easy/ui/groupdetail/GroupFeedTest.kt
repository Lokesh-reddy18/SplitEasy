package com.split.easy.ui.groupdetail

import com.split.easy.domain.balance.rupees
import com.split.easy.domain.model.Category
import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.GroupEvent
import com.split.easy.domain.model.GroupEventType
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.model.SplitType
import com.split.easy.domain.testSettlement
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupFeedTest {

    private val sat = LocalDate.of(2026, 10, 10)
    private val fri = sat.minusDays(1)
    private val members = listOf("you", "priya", "meera", "ravi", "arjun").mapIndexed { i, id ->
        Member(
            id,
            id.replaceFirstChar { it.uppercase() },
            userId = if (id ==
                "arjun"
            ) {
                null
            } else {
                "uid-$id"
            },
            createdAt = i.toLong(),
        )
    }

    private fun equal(id: String, payer: String, rupees: Long, date: LocalDate, createdAt: Long) =
        Expense(
            id = id,
            groupId = "g1",
            paidByMemberId = payer,
            amountPaise = rupees(rupees),
            description = id,
            category = Category.TRAVEL,
            date = date,
            splitType = SplitType.EQUAL,
            sharesPaise = members.associate { it.id to rupees(rupees) / members.size },
            createdByUserId = "uid-you",
            createdAt = createdAt,
        )

    /** The feed in design 03. */
    private val coorg = listOf(
        equal("Cab to Coorg", "meera", 12_600, sat, createdAt = 3),
        equal("Dinner at Raintree", "priya", 2_400, sat, createdAt = 2),
        equal("Fuel", "arjun", 3_000, sat, createdAt = 1),
        equal("Homestay booking", "you", 6_000, fri, createdAt = 0),
    )

    private fun feed(
        expenses: List<Expense> = coorg,
        settlements: List<com.split.easy.domain.model.Settlement> = emptyList(),
        events: List<GroupEvent> = emptyList(),
        archived: Boolean = false,
    ) = buildFeed(members, "you", expenses, settlements, events, archived, ZoneOffset.UTC)

    @Test
    fun `design example - days newest first, with what you borrowed and lent`() {
        val days = feed()
        assertEquals(listOf(sat, fri), days.map { it.date })

        val saturday = days[0].items.map { it as FeedItem.ExpenseItem }
        assertEquals(
            listOf("Cab to Coorg", "Dinner at Raintree", "Fuel"),
            saturday.map {
                it.expense.description
            },
        )
        assertEquals(
            listOf(rupees(2_520), rupees(480), rupees(600)).map { ExpenseImpact.Borrowed(it) },
            saturday.map { it.impact },
        )
        assertEquals(listOf("Meera", "Priya", "Arjun"), saturday.map { it.payerName })

        val homestay = days[1].items.single() as FeedItem.ExpenseItem
        assertNull(homestay.payerName) // "You paid ₹6,000"
        assertEquals(ExpenseImpact.Lent(rupees(4_800)), homestay.impact)
    }

    @Test
    fun `expenses you're not part of, and ones you paid only for yourself`() {
        val snacks = equal(
            "Snacks",
            "meera",
            900,
            sat,
            5,
        ).copy(sharesPaise = mapOf("meera" to 45_000, "ravi" to 45_000))
        val mine = equal("Book", "you", 300, sat, 6).copy(sharesPaise = mapOf("you" to 30_000))
        assertEquals(ExpenseImpact.NotInvolved, impactOf(snacks, "you"))
        assertEquals(ExpenseImpact.NoBalance, impactOf(mine, "you"))
        assertEquals(ExpenseImpact.NotInvolved, impactOf(snacks, myMemberId = null))
    }

    @Test
    fun `deleted expenses and settlements are left out`() {
        val deleted = coorg.first().copy(isDeleted = true)
        val cancelled =
            testSettlement("ravi", "you", 100, SettlementStatus.PENDING, isDeleted = true)
        val items = feed(expenses = listOf(deleted), settlements = listOf(cancelled)).flatMap {
            it.items
        }
        assertEquals(emptyList<FeedItem>(), items)
    }

    @Test
    fun `settlements and events join the feed in time order`() {
        val pending = testSettlement(
            "ravi",
            "you",
            rupees(1_200),
            SettlementStatus.PENDING,
            id = "s1",
        )
            .copy(
                date = sat,
                updatedAt =
                    sat.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() + 10,
            )
        val event =
            GroupEvent(
                "e1",
                "g1",
                "priya",
                "arjun",
                GroupEventType.GUEST_UPI_CHANGED,
                null,
                "arjun@oksbi",
                createdAt = 4,
            )
        val eventTime = sat.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() + 4
        val items = feed(
            settlements = listOf(pending),
            events = listOf(event.copy(createdAt = eventTime)),
        ).first().items

        val settlement = items.first() as FeedItem.SettlementItem
        assertEquals("Ravi", settlement.fromName)
        assertNull(settlement.toName)
        assertFalse(settlement.canUndo) // pending payments are confirmed or cancelled, not undone

        val eventItem = items.filterIsInstance<FeedItem.EventItem>().single()
        assertEquals("Priya", eventItem.actorName)
        assertEquals("Arjun", eventItem.targetName)
    }

    @Test
    fun `only the payer or payee can undo a confirmed payment, and not in an archived group`() {
        val mine = testSettlement(
            "ravi",
            "you",
            100,
            SettlementStatus.CONFIRMED,
            id = "a",
        ).copy(date = sat)
        val others = testSettlement(
            "ravi",
            "priya",
            100,
            SettlementStatus.CONFIRMED,
            id = "b",
        ).copy(date = sat)
        val guests = testSettlement(
            "arjun",
            "arjun",
            100,
            SettlementStatus.CONFIRMED,
            id = "c",
        ).copy(date = sat)

        val items = feed(expenses = emptyList(), settlements = listOf(mine, others)).single().items
            .map { it as FeedItem.SettlementItem }
        assertEquals(
            mapOf("a" to true, "b" to false),
            items.associate {
                it.settlement.id to
                    it.canUndo
            },
        )

        val archived = feed(expenses = emptyList(), settlements = listOf(mine), archived = true)
        assertFalse((archived.single().items.single() as FeedItem.SettlementItem).canUndo)
        // Unrealistic data (paying yourself) must not crash the feed.
        assertTrue(guests.fromMemberId == guests.toMemberId)
        feed(expenses = emptyList(), settlements = listOf(guests))
    }
}
