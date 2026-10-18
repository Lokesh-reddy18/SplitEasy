package com.split.easy.data.demo

import com.split.easy.data.repository.OfflineFirstExpenseRepository
import com.split.easy.data.repository.OfflineFirstGroupRepository
import com.split.easy.data.repository.OfflineFirstMemberRepository
import com.split.easy.data.repository.OfflineFirstSettlementRepository
import com.split.easy.data.repository.RepositoryTest
import com.split.easy.domain.balance.SuggestedPayment
import com.split.easy.domain.usecase.ObserveGroupBalances
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DemoDataLoaderTest : RepositoryTest() {

    @Test
    fun `demo data reproduces the Coorg trip balances from the designs`() = runTest {
        val groups = OfflineFirstGroupRepository(db, currentUser, ids, movingClock, syncScheduler)
        val members = OfflineFirstMemberRepository(db, currentUser, ids, movingClock, syncScheduler)
        val expenses =
            OfflineFirstExpenseRepository(db, currentUser, ids, movingClock, syncScheduler)
        val settlements =
            OfflineFirstSettlementRepository(db, currentUser, ids, movingClock, syncScheduler)

        DemoDataLoader(groups, members, expenses, settlements, movingClock).load()

        val all = groups.observeActiveGroups().first()
        assertEquals(
            setOf("Coorg trip", "Office lunches", "Flat 4B", "Home"),
            all.map {
                it.name
            }.toSet(),
        )

        val coorg = all.first { it.name == "Coorg trip" }
        val balances = ObserveGroupBalances(
            members,
            expenses,
            settlements,
            currentUser,
        )(coorg.id).first()
        val name = balances.members.associate { it.id to it.displayName }
        assertEquals(
            mapOf(
                "You" to 1_20_000L,
                "Priya" to 30_000L,
                "Meera" to 2_00_000L,
                "Ravi" to -1_70_000L,
                "Arjun" to -1_80_000L,
            ),
            balances.netBalances.mapKeys { name.getValue(it.key) },
        )
        assertEquals(
            listOf("Arjun→Meera 180000", "Ravi→You 120000", "Ravi→Priya 30000", "Ravi→Meera 20000"),
            balances.suggestions.map { s: SuggestedPayment ->
                "${name[s.fromMemberId]}→${name[s.toMemberId]} ${s.amountPaise}"
            },
        )
        assertEquals(1, balances.pendingSettlements.size)
    }
}
