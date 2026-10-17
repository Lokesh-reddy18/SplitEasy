package com.split.easy.domain.repository

import com.split.easy.domain.model.Expense
import com.split.easy.domain.model.ExpenseDraft
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    /** Live expenses with their shares, newest first (date, then time added). */
    fun observeExpenses(groupId: String): Flow<List<Expense>>

    fun observeExpense(expenseId: String): Flow<Expense?>

    /** Adds an expense by the current user. Returns its id. */
    suspend fun addExpense(draft: ExpenseDraft): String

    /** Replaces an expense's details and shares, keeping who added it and when. */
    suspend fun updateExpense(expenseId: String, draft: ExpenseDraft)

    suspend fun deleteExpense(expenseId: String)
}
