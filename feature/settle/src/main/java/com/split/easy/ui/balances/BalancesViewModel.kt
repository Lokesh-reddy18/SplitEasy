package com.split.easy.ui.balances

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.repository.GroupRepository
import com.split.easy.domain.repository.SettlementRepository
import com.split.easy.domain.usecase.ObserveGroupBalances
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BalancesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    groupRepository: GroupRepository,
    private val settlementRepository: SettlementRepository,
    observeGroupBalances: ObserveGroupBalances,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    val uiState: StateFlow<BalancesUiState> =
        combine(groupRepository.observeGroup(groupId), observeGroupBalances(groupId)) {
                group,
                balances,
            ->
            buildBalances(group?.name.orEmpty(), group?.isArchived ?: false, balances)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BalancesUiState())

    fun onConfirm(settlementId: String) = setStatus(settlementId, SettlementStatus.CONFIRMED)

    /** "Not received": rejected payments stay in the feed but don't change balances (F4). */
    fun onReject(settlementId: String) = setStatus(settlementId, SettlementStatus.REJECTED)

    /** The payer cancels their own pending payment (F4). */
    fun onCancel(settlementId: String) {
        viewModelScope.launch { settlementRepository.deleteSettlement(settlementId) }
    }

    private fun setStatus(id: String, status: SettlementStatus) {
        viewModelScope.launch { settlementRepository.setStatus(id, status) }
    }
}
