package com.split.easy.data.demo

import com.split.easy.domain.model.Category
import com.split.easy.domain.model.ExpenseDraft
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.model.SplitType
import com.split.easy.domain.repository.ExpenseRepository
import com.split.easy.domain.repository.GroupRepository
import com.split.easy.domain.repository.MemberRepository
import com.split.easy.domain.repository.SettlementRepository
import com.split.easy.domain.split.SplitCalculator
import com.split.easy.domain.split.SplitResult
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Creates the sample groups from the designs, for testing and screenshots (M2-Q9).
 * Only reachable from Profile in debug builds. Everything goes through the normal repositories.
 */
class DemoDataLoader @Inject constructor(
    private val groups: GroupRepository,
    private val members: MemberRepository,
    private val expenses: ExpenseRepository,
    private val settlements: SettlementRepository,
    private val clock: Clock,
) {
    suspend fun load() {
        val today = LocalDate.now(clock)

        // Coorg trip (03, 05): the same numbers as the design, including Ravi's pending UPI payment.
        val coorg = groups.createGroup("Coorg trip", listOf("Priya", "Meera", "Ravi", "Arjun"))
        val c = idsByName(coorg)
        val everyone = c.values.toList()
        addEqual(
            coorg,
            "Homestay booking",
            Category.STAY,
            6_000,
            c.getValue("You"),
            everyone,
            today.minusDays(1),
        )
        addEqual(coorg, "Fuel", Category.FUEL, 3_000, c.getValue("Arjun"), everyone, today)
        addEqual(
            coorg,
            "Dinner at Raintree",
            Category.FOOD,
            2_400,
            c.getValue("Priya"),
            everyone,
            today,
        )
        addEqual(
            coorg,
            "Cab to Coorg",
            Category.TRAVEL,
            12_600,
            c.getValue("Meera"),
            everyone,
            today,
        )
        settle(
            coorg,
            c.getValue("Priya"),
            c.getValue("Meera"),
            2_700,
            SettlementStatus.CONFIRMED,
            SettlementMethod.CASH,
        )
        settle(
            coorg,
            c.getValue("Ravi"),
            c.getValue("Meera"),
            3_100,
            SettlementStatus.CONFIRMED,
            SettlementMethod.UPI,
        )
        val ravi = c.getValue("Ravi")
        settle(
            coorg,
            ravi,
            c.getValue("You"),
            1_200,
            SettlementStatus.PENDING,
            SettlementMethod.UPI,
            "481022756631",
        )
        members.setGuestUpiId(c.getValue("Arjun"), c.getValue("Priya"), "arjun@oksbi")

        // Office lunches (06): you owe Priya Sharma ₹600.
        val officeGuests = listOf("Priya Sharma", "Karan Mehta", "Neha", "Rahul", "Sana")
        val office = groups.createGroup("Office lunches", officeGuests)
        val o = idsByName(office)
        members.setGuestUpiId(o.getValue("Priya Sharma"), o.getValue("You"), "priya.s@okicici")
        val lunchPair = listOf(o.getValue("You"), o.getValue("Priya Sharma"))
        addEqual(
            office,
            "Thali Thursday",
            Category.FOOD,
            1_200,
            o.getValue("Priya Sharma"),
            lunchPair,
            today,
        )

        // Flat 4B: you're owed.
        val flat = groups.createGroup("Flat 4B", listOf("Rohan", "Isha", "Kabir"))
        val f = idsByName(flat)
        addEqual(
            flat,
            "October rent",
            Category.RENT,
            24_000,
            f.getValue("You"),
            f.values.toList(),
            today.minusDays(5),
        )
        addEqual(
            flat,
            "Electricity",
            Category.UTILITIES,
            1_400,
            f.getValue("You"),
            f.values.toList(),
            today.minusDays(3),
        )

        // Home: settled up.
        groups.createGroup("Home", listOf("Aditi"))
    }

    private suspend fun idsByName(groupId: String): Map<String, String> =
        members.observeMembers(groupId).first().associate { it.displayName to it.id }

    private suspend fun addEqual(
        groupId: String,
        description: String,
        category: Category,
        rupees: Long,
        paidBy: String,
        participants: List<String>,
        date: LocalDate,
    ) {
        val total = rupees * 100
        val shares = (
            SplitCalculator.equal(
                total,
                participants,
                paidBy,
            ) as SplitResult.Success
            ).sharesPaise
        expenses.addExpense(
            ExpenseDraft(
                groupId,
                paidBy,
                total,
                description,
                category,
                date,
                SplitType.EQUAL,
                shares,
            ),
        )
    }

    private suspend fun settle(
        groupId: String,
        from: String,
        to: String,
        rupees: Long,
        status: SettlementStatus,
        method: SettlementMethod,
        txnId: String? = null,
    ) {
        settlements.recordSettlement(groupId, from, to, rupees * 100, method, status, txnId)
    }
}
