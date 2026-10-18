package com.split.easy.ui.groupsettings

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.split.easy.domain.balance.equalExpense
import com.split.easy.domain.balance.rupees
import com.split.easy.domain.group.GroupCheck
import com.split.easy.domain.model.CurrentUser
import com.split.easy.domain.model.Group
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.testSettlement
import com.split.easy.domain.usecase.ObserveGroupBalances
import com.split.easy.testing.FakeCurrentUser
import com.split.easy.testing.FakeExpenseRepository
import com.split.easy.testing.FakeGroupRepository
import com.split.easy.testing.FakeMemberRepository
import com.split.easy.testing.FakeSettlementRepository
import com.split.easy.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GroupSettingsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val groups = FakeGroupRepository().apply {
        groups.value =
            listOf(Group("g1", "Coorg trip", "uid-you"))
    }
    private val members = FakeMemberRepository().apply {
        members.value = mapOf(
            "g1" to listOf(
                Member("you", "You", "uid-you", createdAt = 1),
                Member("priya", "Priya", "uid-priya", createdAt = 2),
                Member("arjun", "Arjun", null, createdAt = 3),
                Member("nikhil", "Nikhil", null, createdAt = 4),
            ),
        )
    }

    // Priya owes you ₹300; Arjun and Nikhil are settled.
    private val expenses = FakeExpenseRepository().apply {
        expenses.value =
            mapOf("g1" to listOf(equalExpense("cab", "you", 600, listOf("you", "priya"))))
    }
    private val settlements = FakeSettlementRepository()

    /** The screen keeps uiState alive while it's shown; the test does the same. */
    private fun TestScope.viewModel(
        user: CurrentUser = CurrentUser("uid-you", "You"),
    ): GroupSettingsViewModel {
        val vm = GroupSettingsViewModel(
            SavedStateHandle(mapOf("groupId" to "g1")),
            groups,
            members,
            settlements,
            ObserveGroupBalances(members, expenses, settlements, FakeCurrentUser(user)),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun `design 12 - creator sees who can be removed and why archive is blocked`() = runTest {
        val state = viewModel().uiState.value
        assertTrue(state.isCreator)
        val checks = state.members.associate { it.member.id to it.removeCheck }
        assertEquals(GroupCheck.SettleFirst(rupees(300)), checks["priya"])
        assertEquals(GroupCheck.Allowed, checks["nikhil"])
        assertEquals(GroupCheck.NotAllowed, checks["you"])
        assertEquals(GroupCheck.SettleFirst(rupees(300)), state.archiveCheck)
        assertEquals(GroupCheck.CreatorMustArchive, state.leaveCheck)
    }

    @Test
    fun `pending payments block removing that member`() = runTest {
        settlements.settlements.value =
            mapOf("g1" to listOf(testSettlement("nikhil", "you", 100, SettlementStatus.PENDING)))
        val state = viewModel().uiState.value
        assertEquals(
            GroupCheck.ResolvePendingFirst,
            state.members.first {
                it.member.id == "nikhil"
            }.removeCheck,
        )
    }

    @Test
    fun `rename, add guest, guest UPI ID and remove go to the repositories`() = runTest {
        val vm = viewModel()
        vm.onRename("Coorg 2026")
        vm.onAddGuest("Ravi")
        vm.onSetGuestUpiId("arjun", "arjun@oksbi")
        vm.onRemove("nikhil")
        vm.onRemove("priya") // blocked: owes ₹300

        assertEquals(listOf("g1" to "Ravi"), members.addedGuests)
        assertEquals(listOf(Triple("arjun", "you", "arjun@oksbi")), members.guestUpiChanges)
        assertEquals(listOf("nikhil"), members.removed)
    }

    @Test
    fun `a settled member can leave and the app goes home`() = runTest {
        expenses.expenses.value = emptyMap()
        val vm = viewModel(CurrentUser("uid-priya", "Priya"))
        val state = vm.uiState.value
        assertFalse(state.isCreator)
        assertEquals(GroupCheck.Allowed, state.leaveCheck)
        assertEquals(GroupCheck.NotAllowed, state.archiveCheck)

        vm.leftEvents.test {
            vm.onLeave()
            awaitItem()
        }
        assertEquals(listOf("priya"), members.removed)
    }

    @Test
    fun `archive once everyone is settled, then unarchive`() = runTest {
        expenses.expenses.value = emptyMap()
        val vm = viewModel()
        assertEquals(GroupCheck.Allowed, vm.uiState.value.archiveCheck)
        vm.onArchive()
        assertTrue(groups.groups.value.single().isArchived)
        assertEquals(GroupCheck.Allowed, vm.uiState.value.unarchiveCheck)
        assertEquals(GroupCheck.ReadOnly, vm.uiState.value.memberActionCheck)
        vm.onUnarchive()
        assertFalse(groups.groups.value.single().isArchived)
    }
}
