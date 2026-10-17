package com.split.easy.data.local

import app.cash.turbine.test
import com.split.easy.data.local.entity.ExpenseShareEntity
import com.split.easy.domain.model.Category
import com.split.easy.domain.model.SplitType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseDaoTest : DatabaseTest() {

    private val dao get() = db.expenseDao()

    private fun share(expenseId: String, memberId: String, paise: Long, basisPoints: Int? = null) =
        ExpenseShareEntity(expenseId, memberId, paise, basisPoints)

    @Test
    fun `feed is newest date first, then most recently added, without deleted expenses`() =
        runTest {
            dao.saveWithShares(
                expense("homestay", date = OCT_10.minusDays(1), createdAt = 1),
                emptyList(),
            )
            dao.saveWithShares(expense("cab", date = OCT_10, createdAt = 2), emptyList())
            dao.saveWithShares(expense("fuel", date = OCT_10, createdAt = 4), emptyList())
            dao.saveWithShares(expense("dinner", date = OCT_10, createdAt = 3), emptyList())
            dao.saveWithShares(
                expense("deleted", date = OCT_10, createdAt = 5, isDeleted = true),
                emptyList(),
            )

            assertEquals(
                listOf("fuel", "dinner", "cab", "homestay"),
                dao.observeFeed("g1").first().map { it.id },
            )
        }

    @Test
    fun `saving again replaces the shares`() = runTest {
        val dinner = expense("dinner")
        dao.saveWithShares(
            dinner,
            listOf(share("dinner", "you", 1_20_000), share("dinner", "priya", 1_20_000)),
        )
        dao.saveWithShares(
            dinner.copy(splitType = SplitType.PERCENT, updatedAt = 2),
            listOf(
                share("dinner", "priya", 80_000, 3_333),
                share("dinner", "meera", 1_60_000, 6_667),
            ),
        )

        val saved = dao.observeExpense("dinner").first()!!
        assertEquals(SplitType.PERCENT, saved.expense.splitType)
        assertEquals(
            setOf(
                share("dinner", "priya", 80_000, 3_333),
                share("dinner", "meera", 1_60_000, 6_667),
            ),
            saved.shares.toSet(),
        )
    }

    @Test
    fun `a failed save leaves the old expense and shares untouched`() = runTest {
        val dinner = expense("dinner")
        val original = listOf(share("dinner", "you", 2_40_000))
        dao.saveWithShares(dinner, original)

        // Two shares for the same member break the primary key, so the transaction rolls back.
        val duplicate = listOf(share("dinner", "you", 1), share("dinner", "you", 2))
        runCatching { dao.saveWithShares(dinner.copy(amountPaise = 3), duplicate) }

        val saved = dao.observeExpense("dinner").first()!!
        assertEquals(2_40_000L, saved.expense.amountPaise)
        assertEquals(original, saved.shares)
    }

    @Test
    fun `dates and enums survive a round trip`() = runTest {
        val cab = expense("cab").copy(category = Category.TRAVEL, splitType = SplitType.EXACT)
        dao.saveWithShares(cab, emptyList())
        assertEquals(cab, dao.observeExpense("cab").first()!!.expense)
    }

    @Test
    fun `deleting an expense updates the feed and balances inputs`() = runTest {
        dao.saveWithShares(expense("fuel"), listOf(share("fuel", "you", 2_40_000)))

        dao.observeWithShares("g1").test {
            assertEquals(1, awaitItem().size)
            dao.softDelete("fuel", updatedAt = 7)
            assertEquals(0, awaitItem().size)
        }
        assertNull(dao.observeExpense("fuel").first())
    }
}
