package com.split.easy.sync.upload

import com.split.easy.data.local.dao.SyncDao
import com.split.easy.data.local.entity.GroupEventEntity
import com.split.easy.data.local.entity.MemberEntity
import com.split.easy.sync.remote.RemoteDataSource
import com.split.easy.sync.remote.RemoteWrite
import com.split.easy.sync.remote.toRemote
import javax.inject.Inject

/**
 * Uploads every PENDING row to Firestore (FR-10).
 *
 * Writes go in batches of up to 500 documents, in this order: groups, then members (each with the
 * event for a guest's UPI ID change, which the security rules require in the same batch), then
 * expenses, settlements and any other events. After a batch is committed, its rows are marked
 * SYNCED, but only if they haven't changed since they were read.
 */
class Uploader @Inject constructor(
    private val syncDao: SyncDao,
    private val remote: RemoteDataSource,
) {
    /** Returns how many documents were uploaded. Throws if Firestore fails; the worker retries. */
    suspend fun upload(): Int {
        var uploaded = 0
        for (batch in pendingItems().chunkedBySize()) {
            remote.write(batch.fold(RemoteWrite()) { all, item -> all + item.write })
            batch.forEach { it.markSynced() }
            uploaded += batch.sumOf { it.write.size }
        }
        return uploaded
    }

    /** Documents that must land in the same Firestore batch, and how to mark them SYNCED after. */
    private class UploadItem(val write: RemoteWrite, val markSynced: suspend () -> Unit)

    private suspend fun pendingItems(): List<UploadItem> {
        val items = mutableListOf<UploadItem>()

        syncDao.pendingGroups().forEach { group ->
            val remoteGroup = group.toRemote(syncDao.memberUserIds(group.id))
            items += UploadItem(RemoteWrite(groups = listOf(remoteGroup))) {
                syncDao.markGroupSynced(group.id, group.updatedAt)
            }
        }

        val events = syncDao.pendingEvents().toMutableList()
        syncDao.pendingMembers().forEach { member ->
            val event = events.firstOrNull { it.records(member) }?.also { events.remove(it) }
            val write =
                RemoteWrite(
                    members = listOf(member.toRemote().copy(upiEventId = event?.id)),
                    events = listOfNotNull(event?.toRemote()),
                )
            items += UploadItem(write) {
                syncDao.markMemberSynced(member.id, member.updatedAt)
                event?.let { syncDao.markEventSynced(it.id) }
            }
        }

        syncDao.pendingExpenses().forEach { (expense, shares) ->
            items += UploadItem(RemoteWrite(expenses = listOf(expense.toRemote(shares)))) {
                syncDao.markExpenseSynced(expense.id, expense.updatedAt)
            }
        }
        syncDao.pendingSettlements().forEach { settlement ->
            items += UploadItem(RemoteWrite(settlements = listOf(settlement.toRemote()))) {
                syncDao.markSettlementSynced(settlement.id, settlement.updatedAt)
            }
        }
        events.forEach { event ->
            items +=
                UploadItem(RemoteWrite(events = listOf(event.toRemote()))) {
                    syncDao.markEventSynced(event.id)
                }
        }
        return items
    }

    /** Splits items into batches of at most 500 documents, never splitting an item. */
    private fun List<UploadItem>.chunkedBySize(): List<List<UploadItem>> {
        val batches = mutableListOf<MutableList<UploadItem>>()
        var size = 0
        for (item in this) {
            if (batches.isEmpty() || size + item.write.size > RemoteDataSource.MAX_BATCH_SIZE) {
                batches += mutableListOf<UploadItem>()
                size = 0
            }
            batches.last() += item
            size += item.write.size
        }
        return batches
    }

    /** The event that records this guest's current UPI ID. */
    private fun GroupEventEntity.records(member: MemberEntity) =
        targetMemberId == member.id && newValue == member.upiId

    private operator fun RemoteWrite.plus(other: RemoteWrite) = RemoteWrite(
        groups = groups + other.groups,
        members = members + other.members,
        expenses = expenses + other.expenses,
        settlements = settlements + other.settlements,
        events = events + other.events,
    )
}
