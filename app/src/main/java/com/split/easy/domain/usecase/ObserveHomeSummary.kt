package com.split.easy.domain.usecase

import com.split.easy.domain.model.Group
import com.split.easy.domain.repository.GroupRepository
import com.split.easy.domain.repository.SettlementRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** One row on Groups home. */
data class GroupSummary(
    val group: Group,
    val memberCount: Int,
    /** Positive: you are owed. Negative: you owe. */
    val myNetPaise: Long,
    /** Payments waiting for you to confirm (the badge, F7). */
    val waitingForMe: Int,
)

/** Everything on Groups home: one row per active group plus the totals card. */
data class HomeSummary(val groups: List<GroupSummary>) {
    /** "You owe": the sum of groups where you owe, as a positive amount. */
    val owePaise: Long = groups.filter { it.myNetPaise < 0 }.sumOf { -it.myNetPaise }

    /** "You are owed": the sum of groups where you're owed. */
    val owedPaise: Long = groups.filter { it.myNetPaise > 0 }.sumOf { it.myNetPaise }

    /** "Overall": positive means you are owed. Archived groups aren't included (F6). */
    val netPaise: Long = owedPaise - owePaise
}

/**
 * Builds Groups home from each active group's balances. Balances always come from the tested
 * domain calculator (via [ObserveGroupBalances]) rather than separate SQL, so there's one
 * source of truth for money.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveHomeSummary @Inject constructor(
    private val groupRepository: GroupRepository,
    private val settlementRepository: SettlementRepository,
    private val observeGroupBalances: ObserveGroupBalances,
) {
    operator fun invoke(): Flow<HomeSummary> {
        val rows: Flow<List<Pair<Group, GroupBalances>>> =
            groupRepository.observeActiveGroups().flatMapLatest { groups ->
                if (groups.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        groups.map { group ->
                            observeGroupBalances(group.id).map { group to it }
                        },
                    ) {
                        it.toList()
                    }
                }
            }
        return combine(rows, settlementRepository.observeWaitingForMyConfirmation()) {
                groups,
                waiting,
            ->
            HomeSummary(
                groups.map { (group, balances) ->
                    GroupSummary(
                        group = group,
                        memberCount = balances.members.count { !it.isRemoved },
                        myNetPaise = balances.myNetPaise,
                        waitingForMe = waiting[group.id] ?: 0,
                    )
                },
            )
        }
    }
}
