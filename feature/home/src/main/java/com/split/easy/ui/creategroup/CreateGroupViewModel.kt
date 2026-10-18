package com.split.easy.ui.creategroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateGroupUiState(
    val creatorName: String,
    val name: String = "",
    val guestInput: String = "",
    val guests: List<String> = emptyList(),
    /** The name that was rejected because it's already in the group, shown under the field. */
    val duplicateGuest: String? = null,
    val isSaving: Boolean = false,
) {
    val canCreate: Boolean
        get() = name.isNotBlank() && !isSaving

    val canAddGuest: Boolean
        get() = guestInput.isNotBlank()

    /** You plus the guests. */
    val peopleCount: Int
        get() = guests.size + 1
}

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    currentUserProvider: CurrentUserProvider,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            CreateGroupUiState(creatorName = currentUserProvider.currentUser().displayName),
        )
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    private val created = Channel<String>(Channel.BUFFERED)

    /** Emits the new group's id once it's saved, so the screen can open it. */
    val createdGroupId = created.receiveAsFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name.take(MAX_GROUP_NAME)) }
    }

    fun onGuestInputChange(input: String) {
        _uiState.update { it.copy(guestInput = input.take(MAX_PERSON_NAME), duplicateGuest = null) }
    }

    /** Adds the typed name, unless it's blank or someone with that name is already in the group. */
    fun onAddGuest() {
        _uiState.update { state ->
            val name = state.guestInput.trim()
            // "You" is how the creator is shown, so it can't be a guest's name either.
            val taken = (state.guests + state.creatorName + YOU).any {
                it.equals(name, ignoreCase = true)
            }
            when {
                name.isEmpty() -> state

                taken -> state.copy(duplicateGuest = name)

                else -> state.copy(
                    guests = state.guests + name,
                    guestInput = "",
                    duplicateGuest = null,
                )
            }
        }
    }

    fun onRemoveGuest(name: String) {
        _uiState.update { it.copy(guests = it.guests - name) }
    }

    fun onCreate() {
        // A name typed but not added yet counts as added, so it isn't silently lost.
        if (_uiState.value.guestInput.isNotBlank()) onAddGuest()
        val state = _uiState.value
        if (!state.canCreate || state.duplicateGuest != null) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            created.send(groupRepository.createGroup(state.name, state.guests))
        }
    }

    companion object {
        const val MAX_GROUP_NAME = 40
        const val MAX_PERSON_NAME = 30
        private const val YOU = "You"
    }
}
