package com.split.easy.data.repository

import androidx.room.withTransaction
import com.split.easy.data.IdGenerator
import com.split.easy.data.local.SplitEasyDatabase
import com.split.easy.data.local.entity.ExpenseEntity
import com.split.easy.data.local.entity.SyncState
import com.split.easy.data.mapper.toDomain
import com.split.easy.data.mapper.toShareEntities
import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.ExpenseDraft
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.repository.ExpenseRepository
import com.split.easy.domain.sync.SyncScheduler
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstExpenseRepository @Inject constructor(
    private val db: SplitEasyDatabase,
    private val currentUser: CurrentUserProvider,
    private val ids: IdGenerator,
    private val clock: Clock,
    private val syncScheduler: SyncScheduler,
) : ExpenseRepository {

    private val expenseDao = db.expenseDao()

    override fun observeExpenses(groupId: String): Flow<List<Expense>> =
        expenseDao.observeWithShares(groupId).map { expenses -> expenses.map { it.toDomain() } }

    override fun observeExpense(expenseId: String): Flow<Expense?> =
        expenseDao.observeExpense(expenseId).map { it?.toDomain() }

    override suspend fun addExpense(draft: ExpenseDraft): String {
        val now = clock.millis()
        val expense = draft.toEntity(
            id = ids.newId(),
            createdBy = currentUser.currentUser().userId,
            createdAt = now,
            updatedAt = now,
        )
        save(expense, draft, now)
        return expense.id
    }

    override suspend fun updateExpense(expenseId: String, draft: ExpenseDraft) {
        val now = clock.millis()
        val existing = requireNotNull(expenseDao.getExpense(expenseId)) { "No expense $expenseId" }
        // Who added it and when never change; only the details do.
        val expense = draft.toEntity(
            id = expenseId,
            createdBy = existing.createdBy,
            createdAt = existing.createdAt,
            updatedAt = now,
        )
        save(expense, draft, now)
    }

    override suspend fun deleteExpense(expenseId: String) {
        val now = clock.millis()
        db.withTransaction {
            val expense =
                requireNotNull(expenseDao.getExpense(expenseId)) { "No expense $expenseId" }
            expenseDao.softDelete(expenseId, updatedAt = now)
            db.groupDao().touchActivity(expense.groupId, now)
        }
        syncScheduler.requestSync()
    }

    private suspend fun save(expense: ExpenseEntity, draft: ExpenseDraft, now: Long) {
        db.withTransaction {
            expenseDao.saveWithShares(expense, draft.toShareEntities(expense.id))
            db.groupDao().touchActivity(expense.groupId, now)
        }
        syncScheduler.requestSync()
    }

    private fun ExpenseDraft.toEntity(
        id: String,
        createdBy: String,
        createdAt: Long,
        updatedAt: Long,
    ) = ExpenseEntity(
        id = id,
        groupId = groupId,
        paidBy = paidByMemberId,
        amountPaise = amountPaise,
        description = description.trim(),
        category = category,
        date = date,
        splitType = splitType,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncState = SyncState.PENDING,
    )
}
