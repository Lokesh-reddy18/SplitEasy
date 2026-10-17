package com.split.easy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.split.easy.data.local.entity.ExpenseEntity
import com.split.easy.data.local.entity.ExpenseShareEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    /** Saves an expense and replaces its shares in one transaction, so they never disagree. */
    @Transaction
    suspend fun saveWithShares(expense: ExpenseEntity, shares: List<ExpenseShareEntity>) {
        upsertExpense(expense)
        deleteShares(expense.id)
        insertShares(shares)
    }

    /** Group feed: newest date first, then most recently added (M1-Q5). */
    @Query(
        "SELECT * FROM expenses WHERE groupId = :groupId AND isDeleted = 0 " +
            "ORDER BY date DESC, createdAt DESC",
    )
    fun observeFeed(groupId: String): Flow<List<ExpenseEntity>>

    /** All live expenses with their shares, newest first: the feed and the balance inputs. */
    @Transaction
    @Query(
        "SELECT * FROM expenses WHERE groupId = :groupId AND isDeleted = 0 " +
            "ORDER BY date DESC, createdAt DESC",
    )
    fun observeWithShares(groupId: String): Flow<List<ExpenseWithShares>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpense(id: String): ExpenseEntity?

    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :id AND isDeleted = 0")
    fun observeExpense(id: String): Flow<ExpenseWithShares?>

    @Query(
        "UPDATE expenses SET isDeleted = 1, updatedAt = :updatedAt, syncState = 'PENDING' " +
            "WHERE id = :id",
    )
    suspend fun softDelete(id: String, updatedAt: Long)

    @Upsert
    suspend fun upsertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expense_shares WHERE expenseId = :expenseId")
    suspend fun deleteShares(expenseId: String)

    @Insert
    suspend fun insertShares(shares: List<ExpenseShareEntity>)
}
