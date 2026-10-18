package com.split.easy.ui.expense

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.split.easy.data.preferences.UserPreferencesRepository
import com.split.easy.domain.model.Category
import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.Group
import com.split.easy.domain.model.Member
import com.split.easy.domain.model.SplitType
import com.split.easy.testing.FakeCurrentUser
import com.split.easy.testing.FakeExpenseRepository
import com.split.easy.testing.FakeGroupRepository
import com.split.easy.testing.FakeMemberRepository
import com.split.easy.testing.MainDispatcherRule
import com.split.easy.ui.expense.editor.ExpenseEditorViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExpenseEditorViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val today = LocalDate.of(2026, 10, 10)
    private val clock = Clock.fixed(Instant.parse("2026-10-10T09:00:00Z"), ZoneOffset.UTC)
    private val groups = FakeGroupRepository().apply {
        groups.value =
            listOf(Group("g1", "Coorg trip", "uid-you"))
    }
    private val members = FakeMemberRepository().apply {
        members.value = mapOf(
            "g1" to listOf(
                Member("you", "You", "uid-you", createdAt = 1),
                Member("priya", "Priya", "uid-priya", createdAt = 2),
                Member("ravi", "Ravi", null, createdAt = 3, isRemoved = true),
            ),
        )
    }
    private val expenses = FakeExpenseRepository()
    private val preferences by lazy {
        UserPreferencesRepository(
            PreferenceDataStoreFactory.create {
                folder.newFile("p.preferences_pb").also { it.delete() }
            },
        )
    }

    private fun viewModel(expenseId: String? = null) = ExpenseEditorViewModel(
        SavedStateHandle(
            listOfNotNull(
                "groupId" to "g1",
                expenseId?.let {
                    "expenseId" to it
                },
            ).toMap(),
        ),
        groups,
        members,
        expenses,
        preferences,
        FakeCurrentUser(),
        clock,
    )

    @Test
    fun `new expense starts with current members and you paying`() {
        val state = viewModel().uiState.value
        assertEquals("Coorg trip", state.groupName)
        assertEquals(listOf("you", "priya"), state.members.map { it.id })
        assertEquals("you", state.form!!.paidByMemberId)
        assertEquals(today, state.form!!.date)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `saving an invalid form shows errors instead`() {
        val vm = viewModel()
        vm.onSave()
        assertTrue(vm.uiState.value.showErrors)
        assertEquals(emptyList<Any>(), expenses.added)
    }

    @Test
    fun `saving a valid expense adds it and remembers the group`() = runTest {
        val vm = viewModel()
        vm.onAmountChange("₹2,400")
        vm.onDescriptionChange("Dinner")
        vm.onCategoryChange(Category.FOOD)
        assertTrue(vm.uiState.value.hasChanges)

        vm.savedEvents.test {
            vm.onSave()
            awaitItem()
        }
        val draft = expenses.added.single()
        assertEquals(2_40_000L, draft.amountPaise)
        assertEquals(mapOf("you" to 1_20_000L, "priya" to 1_20_000L), draft.sharesPaise)
        assertEquals("g1", preferences.lastUsedGroupId.first())
    }

    @Test
    fun `editing loads the expense, including someone who has left, and updates it`() = runTest {
        expenses.expenses.value = mapOf(
            "g1" to listOf(
                Expense(
                    id = "e1",
                    groupId = "g1",
                    paidByMemberId = "ravi",
                    amountPaise = 90_000,
                    description = "Snacks",
                    category = Category.FOOD,
                    date = today.minusDays(1),
                    splitType = SplitType.EQUAL,
                    sharesPaise = mapOf("you" to 45_000, "ravi" to 45_000),
                    createdByUserId = "uid-you",
                    createdAt = 0,
                ),
            ),
        )
        val vm = viewModel(expenseId = "e1")
        val state = vm.uiState.value
        assertTrue(state.isEditing)
        assertEquals(listOf("you", "priya", "ravi"), state.members.map { it.id })
        assertEquals("900", state.form!!.amountText)

        vm.onToggleIncluded("priya")
        vm.savedEvents.test {
            vm.onSave()
            awaitItem()
        }
        val (id, draft) = expenses.updated.single()
        assertEquals("e1", id)
        assertEquals(
            mapOf("you" to 30_000L, "priya" to 30_000L, "ravi" to 30_000L),
            draft.sharesPaise,
        )
    }
}
