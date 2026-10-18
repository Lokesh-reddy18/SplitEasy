package com.split.easy.sync.remote

import com.split.easy.sync.remote.FirestoreFields.toMap
import org.junit.Assert.assertEquals
import org.junit.Test

class FirestoreFieldsTest {

    private val group =
        RemoteGroup("g1", "Coorg trip", "uid-a", listOf("uid-a", "uid-b"), false, 5, 4, false)
    private val member =
        RemoteMember("m1", "g1", "Arjun", null, "arjun@oksbi", 1, 2, false, upiEventId = "ev1")
    private val expense = RemoteExpense(
        id = "e1",
        groupId = "g1",
        paidBy = "m1",
        amountPaise = 2_40_000,
        description = "Dinner at Raintree",
        category = "FOOD",
        date = "2026-10-10",
        splitType = "PERCENT",
        createdBy = "uid-a",
        createdAt = 1,
        updatedAt = 2,
        isDeleted = false,
        shares = mapOf("m1" to RemoteShare(1_20_000, 5_000), "m2" to RemoteShare(1_20_000, null)),
    )
    private val settlement =
        RemoteSettlement(
            "s1", "g1", "a", "b", 60_000, "2026-10-10", null, "PENDING", "CASH", 3, false,
        )
    private val event =
        RemoteEvent("ev1", "g1", "a", "b", "GUEST_UPI_CHANGED", "old@ybl", "new@ybl", 8)

    @Test
    fun `every document survives a round trip through Firestore fields`() {
        assertEquals(group, FirestoreFields.group("g1", group.toMap()))
        assertEquals(member, FirestoreFields.member("m1", member.toMap()))
        assertEquals(expense, FirestoreFields.expense("e1", expense.toMap()))
        assertEquals(settlement, FirestoreFields.settlement("s1", settlement.toMap()))
        assertEquals(event, FirestoreFields.event("ev1", event.toMap()))
    }

    @Test
    fun `Firestore's own number types and missing optional fields are accepted`() {
        // Firestore hands back whole numbers as Long and may drop null fields entirely.
        val stored = expense.toMap().toMutableMap().apply {
            put("amountPaise", 2_40_000.toLong())
            put(
                "shares",
                mapOf(
                    "m1" to mapOf("paise" to 1_20_000L, "basisPoints" to 5_000L),
                    "m2" to mapOf("paise" to 1_20_000L),
                ),
            )
        }
        assertEquals(expense, FirestoreFields.expense("e1", stored))

        val memberWithoutNulls = member.toMap().filterValues { it != null }
        assertEquals(member, FirestoreFields.member("m1", memberWithoutNulls))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a document missing a required field is rejected`() {
        FirestoreFields.settlement("s1", settlement.toMap() - "amountPaise")
    }
}
