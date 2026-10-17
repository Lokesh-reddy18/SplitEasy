package com.lokeshreddy.spliteasy.domain.balance

import com.lokeshreddy.spliteasy.domain.model.Expense
import com.lokeshreddy.spliteasy.domain.model.Settlement
import com.lokeshreddy.spliteasy.domain.model.SettlementMethod
import com.lokeshreddy.spliteasy.domain.model.SettlementStatus

/** Rupees to paise, to keep test data readable. */
fun rupees(amount: Long): Long = amount * 100

fun equalExpense(id: String, paidBy: String, totalRupees: Long, members: List<String>): Expense {
    val share = rupees(totalRupees) / members.size
    return Expense(id, paidBy, rupees(totalRupees), members.associateWith { share })
}

fun settlement(
    from: String,
    to: String,
    amountRupees: Long,
    status: SettlementStatus = SettlementStatus.CONFIRMED,
    isDeleted: Boolean = false,
) = Settlement(
    id = "$from-$to-$amountRupees-$status",
    fromMemberId = from,
    toMemberId = to,
    amountPaise = rupees(amountRupees),
    status = status,
    method = SettlementMethod.UPI,
    isDeleted = isDeleted,
)

/**
 * The Coorg trip from the designs (03-group-detail, 05-balances).
 * The two earlier confirmed settlements aren't shown in the designs; they're made up here so the
 * net balances match the Balances screen exactly.
 */
object CoorgTrip {
    val members = listOf("you", "priya", "meera", "ravi", "arjun")

    val expenses = listOf(
        equalExpense("cab", paidBy = "meera", totalRupees = 12_600, members = members),
        equalExpense("dinner", paidBy = "priya", totalRupees = 2_400, members = members),
        equalExpense("fuel", paidBy = "arjun", totalRupees = 3_000, members = members),
        equalExpense("homestay", paidBy = "you", totalRupees = 6_000, members = members),
    )

    val settlements = listOf(
        settlement("priya", "meera", 2_700),
        settlement("ravi", "meera", 3_100),
        // Ravi's UPI payment waiting for your confirmation: it must not change balances.
        settlement("ravi", "you", 1_200, status = SettlementStatus.PENDING),
    )
}
