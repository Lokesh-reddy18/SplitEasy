package com.split.easy.sync.download

import androidx.room.withTransaction
import com.split.easy.data.local.SplitEasyDatabase
import com.split.easy.data.local.dao.MergeDao
import com.split.easy.sync.remote.RemoteGroup
import com.split.easy.sync.remote.RemoteGroupContents
import com.split.easy.sync.remote.toEntities
import com.split.easy.sync.remote.toEntity
import javax.inject.Inject

/** Writes changes from Firestore into Room, record by record, using [shouldTakeRemote]. */
class RemoteMerger @Inject constructor(
    private val db: SplitEasyDatabase,
    private val mergeDao: MergeDao,
) {
    /** Returns how many records changed on this phone. */
    suspend fun mergeGroups(remote: List<RemoteGroup>): Int = db.withTransaction {
        val local = mergeDao.groups(remote.map { it.id }).associateBy { it.id }
        var changed = 0
        for (group in remote) {
            val mine = local[group.id]
            if (shouldTakeRemote(mine?.updatedAt, mine?.syncState, group.updatedAt)) {
                // lastActivityAt only moves forward, whichever side it came from.
                val activity = maxOf(group.lastActivityAt, mine?.lastActivityAt ?: 0)
                mergeDao.upsertGroups(listOf(group.toEntity().copy(lastActivityAt = activity)))
                changed++
            } else {
                // A local rename or archive wins, but newer activity still reorders Groups home.
                mergeDao.touchActivity(group.id, group.lastActivityAt)
            }
        }
        changed
    }

    suspend fun mergeContents(groupId: String, remote: RemoteGroupContents): Int =
        db.withTransaction {
            var changed = 0
            var latestActivity = 0L

            val localMembers = mergeDao.members(remote.members.map { it.id }).associateBy { it.id }
            val members = remote.members.filter {
                shouldTakeRemote(
                    localMembers[it.id]?.updatedAt,
                    localMembers[it.id]?.syncState,
                    it.updatedAt,
                )
            }
            mergeDao.upsertMembers(members.map { it.toEntity() })
            changed += members.size

            val localExpenses = mergeDao.expenses(
                remote.expenses.map {
                    it.id
                },
            ).associateBy { it.id }
            remote.expenses.filter {
                shouldTakeRemote(
                    localExpenses[it.id]?.updatedAt,
                    localExpenses[it.id]?.syncState,
                    it.updatedAt,
                )
            }.forEach {
                val (expense, shares) = it.toEntities()
                mergeDao.replaceExpense(expense, shares)
                latestActivity = maxOf(latestActivity, it.updatedAt)
                changed++
            }

            val localSettlements = mergeDao.settlements(
                remote.settlements.map {
                    it.id
                },
            ).associateBy { it.id }
            val settlements = remote.settlements.filter {
                shouldTakeRemote(
                    localSettlements[it.id]?.updatedAt,
                    localSettlements[it.id]?.syncState,
                    it.updatedAt,
                )
            }
            mergeDao.upsertSettlements(settlements.map { it.toEntity() })
            settlements.forEach { latestActivity = maxOf(latestActivity, it.updatedAt) }
            changed += settlements.size

            // Events are append-only: new ones are added, existing ones are never touched.
            mergeDao.insertEvents(remote.events.map { it.toEntity() })
            remote.events.forEach { latestActivity = maxOf(latestActivity, it.createdAt) }

            if (latestActivity > 0) mergeDao.touchActivity(groupId, latestActivity)
            changed
        }
}
