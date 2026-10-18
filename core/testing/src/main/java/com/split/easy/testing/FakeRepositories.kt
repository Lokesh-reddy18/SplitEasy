package com.split.easy.testing

import com.split.easy.domain.model.CurrentUser
import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.ExpenseDraft
import com.split.easy.domain.model.Group
import com.split.easy.domain.model.GroupEvent
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.Settlement
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.repository.ExpenseRepository
import com.split.easy.domain.repository.GroupRepository
import com.split.easy.domain.repository.MemberRepository
import com.split.easy.domain.repository.SettlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Simple in-memory repositories for ViewModel and use case tests. Only reads are realistic. */

class FakeCurrentUser(private val user: CurrentUser = CurrentUser("uid-you", "You")) :
    CurrentUserProvider {
    override fun currentUser() = user
}

class FakeGroupRepository : GroupRepository {
    val groups = MutableStateFlow<List<Group>>(emptyList())
    val created = mutableListOf<Pair<String, List<String>>>()

    override fun observeActiveGroups() =
        groups.map { all -> all.filter { !it.isArchived }.sortedByDescending { it.lastActivityAt } }

    override fun observeArchivedGroups() = groups.map { all -> all.filter { it.isArchived } }

    override fun observeGroup(groupId: String) = groups.map { all ->
        all.firstOrNull {
            it.id ==
                groupId
        }
    }

    override suspend fun createGroup(name: String, guestNames: List<String>): String {
        created += name to guestNames
        return "new-group-${created.size}"
    }

    override suspend fun renameGroup(groupId: String, name: String) = Unit

    override suspend fun setArchived(groupId: String, isArchived: Boolean) {
        groups.value =
            groups.value.map { if (it.id == groupId) it.copy(isArchived = isArchived) else it }
    }
}

class FakeMemberRepository : MemberRepository {
    val members = MutableStateFlow<Map<String, List<Member>>>(emptyMap())
    val events = MutableStateFlow<List<GroupEvent>>(emptyList())
    val guestUpiChanges = mutableListOf<Triple<String, String, String>>()
    val myUpiIds = mutableListOf<String>()
    val addedGuests = mutableListOf<Pair<String, String>>()
    val removed = mutableListOf<String>()

    private fun updateMember(memberId: String, change: (Member) -> Member) {
        members.value = members.value.mapValues { (_, list) ->
            list.map { if (it.id == memberId) change(it) else it }
        }
    }

    override fun observeMembers(groupId: String) = members.map { it[groupId].orEmpty() }

    override suspend fun addGuest(groupId: String, displayName: String): String {
        addedGuests += groupId to displayName
        val id = "guest-${addedGuests.size}"
        val guest = Member(id, displayName, userId = null, createdAt = 100L + addedGuests.size)
        members.value = members.value + (groupId to members.value[groupId].orEmpty() + guest)
        return id
    }

    override suspend fun removeMember(memberId: String) {
        removed += memberId
        updateMember(memberId) { it.copy(isRemoved = true) }
    }

    override suspend fun setGuestUpiId(
        guestMemberId: String,
        actorMemberId: String,
        upiId: String,
    ) {
        guestUpiChanges += Triple(guestMemberId, actorMemberId, upiId)
        updateMember(guestMemberId) { it.copy(upiId = upiId) }
    }

    override suspend fun setMyUpiId(upiId: String) {
        myUpiIds += upiId
        members.value = members.value.mapValues { (_, list) ->
            list.map { if (it.userId == "uid-you") it.copy(upiId = upiId) else it }
        }
    }

    override fun observeEvents(groupId: String): Flow<List<GroupEvent>> = events
}

class FakeExpenseRepository : ExpenseRepository {
    val expenses = MutableStateFlow<Map<String, List<Expense>>>(emptyMap())
    val added = mutableListOf<ExpenseDraft>()
    val updated = mutableListOf<Pair<String, ExpenseDraft>>()
    val deleted = mutableListOf<String>()

    override fun observeExpenses(groupId: String) = expenses.map { it[groupId].orEmpty() }

    override fun observeExpense(expenseId: String) =
        expenses.map { all -> all.values.flatten().firstOrNull { it.id == expenseId } }

    override suspend fun addExpense(draft: ExpenseDraft): String {
        added += draft
        return "new-expense-${added.size}"
    }

    override suspend fun updateExpense(expenseId: String, draft: ExpenseDraft) {
        updated += expenseId to draft
    }

    override suspend fun deleteExpense(expenseId: String) {
        deleted += expenseId
        expenses.value =
            expenses.value.mapValues { (_, list) -> list.filterNot { it.id == expenseId } }
    }
}

class FakeSettlementRepository : SettlementRepository {
    val settlements = MutableStateFlow<Map<String, List<Settlement>>>(emptyMap())
    val waiting = MutableStateFlow<Map<String, Int>>(emptyMap())

    data class Recorded(
        val from: String,
        val to: String,
        val amountPaise: Long,
        val method: SettlementMethod,
        val status: SettlementStatus,
        val upiTxnId: String?,
    )

    val recorded = mutableListOf<Recorded>()

    override fun observeSettlements(groupId: String) = settlements.map { it[groupId].orEmpty() }

    override suspend fun recordSettlement(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amountPaise: Long,
        method: SettlementMethod,
        status: SettlementStatus,
        upiTxnId: String?,
    ): String {
        recorded += Recorded(fromMemberId, toMemberId, amountPaise, method, status, upiTxnId)
        return "settlement-${recorded.size}"
    }

    override suspend fun setStatus(settlementId: String, status: SettlementStatus) =
        error("not used")

    override suspend fun deleteSettlement(settlementId: String) = error("not used")

    override fun observeWaitingForMyConfirmation() = waiting
}
