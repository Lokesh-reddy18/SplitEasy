package com.split.easy.data.repository

import androidx.room.withTransaction
import com.split.easy.data.IdGenerator
import com.split.easy.data.local.SplitEasyDatabase
import com.split.easy.data.local.entity.GroupEventEntity
import com.split.easy.data.local.entity.MemberEntity
import com.split.easy.data.mapper.toDomain
import com.split.easy.domain.model.GroupEvent
import com.split.easy.domain.model.GroupEventType
import com.split.easy.domain.model.Member
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.repository.MemberRepository
import com.split.easy.sync.SyncScheduler
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstMemberRepository @Inject constructor(
    private val db: SplitEasyDatabase,
    private val currentUser: CurrentUserProvider,
    private val ids: IdGenerator,
    private val clock: Clock,
    private val syncScheduler: SyncScheduler,
) : MemberRepository {

    private val memberDao = db.memberDao()
    private val groupDao = db.groupDao()

    override fun observeMembers(groupId: String): Flow<List<Member>> =
        memberDao.observeAllMembers(groupId).map { members -> members.map { it.toDomain() } }

    override suspend fun addGuest(groupId: String, displayName: String): String {
        val now = clock.millis()
        val guest = MemberEntity(
            id = ids.newId(),
            groupId = groupId,
            displayName = displayName.trim(),
            createdAt = now,
            updatedAt = now,
        )
        db.withTransaction {
            memberDao.upsert(guest)
            groupDao.touchActivity(groupId, now)
        }
        syncScheduler.requestSync()
        return guest.id
    }

    override suspend fun removeMember(memberId: String) {
        val now = clock.millis()
        db.withTransaction {
            val member = requireNotNull(memberDao.getMember(memberId)) { "No member $memberId" }
            memberDao.softDelete(memberId, updatedAt = now)
            groupDao.touchActivity(member.groupId, now)
        }
        syncScheduler.requestSync()
    }

    override suspend fun setGuestUpiId(
        guestMemberId: String,
        actorMemberId: String,
        upiId: String,
    ) {
        val now = clock.millis()
        val newUpiId = upiId.trim()
        db.withTransaction {
            val guest =
                requireNotNull(memberDao.getMember(guestMemberId)) { "No member $guestMemberId" }
            require(guest.userId == null) { "Only a guest's UPI ID can be set by others (F1)" }
            if (guest.upiId == newUpiId) return@withTransaction

            memberDao.updateUpiId(guestMemberId, newUpiId, updatedAt = now)
            // The event is written in the same transaction so the change always shows in the feed.
            db.groupEventDao().insert(
                GroupEventEntity(
                    id = ids.newId(),
                    groupId = guest.groupId,
                    actorMemberId = actorMemberId,
                    targetMemberId = guestMemberId,
                    type = GroupEventType.GUEST_UPI_CHANGED,
                    oldValue = guest.upiId,
                    newValue = newUpiId,
                    createdAt = now,
                ),
            )
            groupDao.touchActivity(guest.groupId, now)
        }
        syncScheduler.requestSync()
    }

    override suspend fun setMyUpiId(upiId: String) {
        val now = clock.millis()
        val userId = currentUser.currentUser().userId
        db.withTransaction {
            memberDao.getMembershipsOfUser(userId).forEach { membership ->
                memberDao.updateUpiId(membership.id, upiId.trim(), updatedAt = now)
            }
        }
        syncScheduler.requestSync()
    }

    override fun observeEvents(groupId: String): Flow<List<GroupEvent>> =
        db.groupEventDao().observeEvents(groupId).map { events -> events.map { it.toDomain() } }
}
