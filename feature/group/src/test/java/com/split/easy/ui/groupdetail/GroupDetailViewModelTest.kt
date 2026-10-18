package com.split.easy.ui.groupdetail

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.split.easy.data.preferences.UserPreferencesRepository
import com.split.easy.domain.balance.equalExpense
import com.split.easy.domain.balance.rupees
import com.split.easy.domain.model.Group
import com.split.easy.domain.model.Member
import com.split.easy.domain.usecase.ObserveGroupBalances
import com.split.easy.testing.FakeCurrentUser
import com.split.easy.testing.FakeExpenseRepository
import com.split.easy.testing.FakeGroupRepository
import com.split.easy.testing.FakeMemberRepository
import com.split.easy.testing.FakeSettlementRepository
import com.split.easy.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GroupDetailViewModelTest {

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
                Member("gone", "Gone", null, createdAt = 3, isRemoved = true),
            ),
        )
    }
    private val expenses = FakeExpenseRepository()
    private val settlements = FakeSettlementRepository()

    @get:Rule
    val folder = TemporaryFolder()

    private val preferences by lazy {
        UserPreferencesRepository(
            PreferenceDataStoreFactory.create {
                folder.newFile("p.preferences_pb").also { it.delete() }
            },
        )
    }

    private val viewModel by lazy {
        GroupDetailViewModel(
            SavedStateHandle(mapOf("groupId" to "g1")),
            groups,
            members,
            expenses,
            settlements,
            ObserveGroupBalances(members, expenses, settlements, FakeCurrentUser()),
            preferences,
        )
    }

    @Test
    fun `combines the group, balances and feed`() = runTest {
        viewModel.uiState.test {
            var empty = awaitItem()
            while (empty.isLoading) empty = awaitItem()
            assertEquals("Coorg trip", empty.groupName)
            assertEquals(listOf(null, "Priya"), empty.memberNames)
            assertTrue(empty.canChangeThings)

            expenses.expenses.value =
                mapOf("g1" to listOf(equalExpense("cab", "priya", 1_000, listOf("you", "priya"))))
            // The feed and the balances update from the same change, possibly one emission apart.
            var withExpense = awaitItem()
            while (withExpense.myNetPaise == 0L ||
                withExpense.feed.isEmpty()
            ) {
                withExpense = awaitItem()
            }
            assertEquals(rupees(-500), withExpense.myNetPaise)
            assertEquals(SettleUpTarget.Pay("priya"), withExpense.settleUpTarget)
            assertEquals(1, withExpense.feed.single().items.size)
        }
    }

    @Test
    fun `UPI ID prompt shows when you're owed and have none, until Not now`() = runTest {
        expenses.expenses.value =
            mapOf("g1" to listOf(equalExpense("cab", "you", 1_000, listOf("you", "priya"))))
        viewModel.uiState.test {
            var state = awaitItem()
            while (!state.showUpiPrompt) state = awaitItem()

            viewModel.onDismissUpiPrompt()
            while (state.showUpiPrompt) state = awaitItem()
        }
    }
}
