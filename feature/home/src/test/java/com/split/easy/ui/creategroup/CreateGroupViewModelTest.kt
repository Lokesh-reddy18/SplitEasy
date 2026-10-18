package com.split.easy.ui.creategroup

import app.cash.turbine.test
import com.split.easy.testing.FakeCurrentUser
import com.split.easy.testing.FakeGroupRepository
import com.split.easy.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CreateGroupViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeGroupRepository()
    private val viewModel = CreateGroupViewModel(repository, FakeCurrentUser())

    private val state get() = viewModel.uiState.value

    @Test
    fun `create needs a name`() {
        assertFalse(state.canCreate)
        viewModel.onNameChange("   ")
        assertFalse(state.canCreate)
        viewModel.onNameChange("Coorg trip")
        assertTrue(state.canCreate)
    }

    @Test
    fun `guests are trimmed and added in order`() {
        viewModel.onGuestInputChange(" Priya ")
        viewModel.onAddGuest()
        viewModel.onGuestInputChange("Meera")
        viewModel.onAddGuest()

        assertEquals(listOf("Priya", "Meera"), state.guests)
        assertEquals("", state.guestInput)
        assertEquals(3, state.peopleCount)
    }

    @Test
    fun `a name already in the group is rejected, ignoring case`() {
        viewModel.onGuestInputChange("Priya")
        viewModel.onAddGuest()
        viewModel.onGuestInputChange("priya")
        viewModel.onAddGuest()
        assertEquals(listOf("Priya"), state.guests)
        assertEquals("priya", state.duplicateGuest)

        // "You" is the creator's name, so it can't be a guest either.
        viewModel.onGuestInputChange("you")
        viewModel.onAddGuest()
        assertEquals("you", state.duplicateGuest)
    }

    @Test
    fun `names are capped in length`() {
        viewModel.onNameChange("x".repeat(100))
        assertEquals(CreateGroupViewModel.MAX_GROUP_NAME, state.name.length)
        viewModel.onGuestInputChange("y".repeat(100))
        assertEquals(CreateGroupViewModel.MAX_PERSON_NAME, state.guestInput.length)
    }

    @Test
    fun `removing a guest takes them off the list`() {
        viewModel.onGuestInputChange("Priya")
        viewModel.onAddGuest()
        viewModel.onRemoveGuest("Priya")
        assertEquals(emptyList<String>(), state.guests)
    }

    @Test
    fun `creating saves the group, including a name typed but not added, and opens it`() = runTest {
        viewModel.onNameChange("Coorg trip")
        viewModel.onGuestInputChange("Priya")
        viewModel.onAddGuest()
        viewModel.onGuestInputChange("Meera")

        viewModel.createdGroupId.test {
            viewModel.onCreate()
            assertEquals("new-group-1", awaitItem())
        }
        assertEquals(listOf("Coorg trip" to listOf("Priya", "Meera")), repository.created)
    }
}
