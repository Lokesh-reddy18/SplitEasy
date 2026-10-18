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

    override fun observeMembers(groupId: String) = members.map { it[groupId].orEmpty() }

    override suspend fun addGuest(groupId: String, displayName: String) = error("not used")

    override suspend fun removeMember(memberId: String) = error("not used")

    override suspend fun setGuestUpiId(
        guestMemberId: String,
        actorMemberId: String,
        upiId: String,
    ) = error("not used")

    override suspend fun setMyUpiId(upiId: String) = error("not used")

    override fun observeEvents(groupId: String): Flow<List<GroupEvent>> =
        MutableStateFlow(emptyList())
}

class FakeExpenseRepository : ExpenseRepository {
    val expenses = MutableStateFlow<Map<String, List<Expense>>>(emptyMap())

    override fun observeExpenses(groupId: String) = expenses.map { it[groupId].orEmpty() }

    override fun observeExpense(expenseId: String) =
        expenses.map { all -> all.values.flatten().firstOrNull { it.id == expenseId } }

    override suspend fun addExpense(draft: ExpenseDraft) = error("not used")

    override suspend fun updateExpense(expenseId: String, draft: ExpenseDraft) = error("not used")

    override suspend fun deleteExpense(expenseId: String) = error("not used")
}

class FakeSettlementRepository : SettlementRepository {
    val settlements = MutableStateFlow<Map<String, List<Settlement>>>(emptyMap())
    val waiting = MutableStateFlow<Map<String, Int>>(emptyMap())

    override fun observeSettlements(groupId: String) = settlements.map { it[groupId].orEmpty() }

    override suspend fun recordSettlement(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amountPaise: Long,
        method: SettlementMethod,
        status: SettlementStatus,
        upiTxnId: String?,
    ) = error("not used")

    override suspend fun setStatus(settlementId: String, status: SettlementStatus) =
        error("not used")

    override suspend fun deleteSettlement(settlementId: String) = error("not used")

    override fun observeWaitingForMyConfirmation() = waiting
}
