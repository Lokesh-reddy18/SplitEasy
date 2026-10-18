package com.split.easy.data.repository

import androidx.room.withTransaction
import com.split.easy.data.IdGenerator
import com.split.easy.data.local.SplitEasyDatabase
import com.split.easy.data.local.entity.GroupEntity
import com.split.easy.data.local.entity.MemberEntity
import com.split.easy.data.mapper.toDomain
import com.split.easy.domain.model.Group
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.repository.GroupRepository
import com.split.easy.domain.sync.SyncScheduler
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstGroupRepository @Inject constructor(
    private val db: SplitEasyDatabase,
    private val currentUser: CurrentUserProvider,
    private val ids: IdGenerator,
    private val clock: Clock,
    private val syncScheduler: SyncScheduler,
) : GroupRepository {

    private val groupDao = db.groupDao()

    override fun observeActiveGroups(): Flow<List<Group>> =
        groupDao.observeActiveGroups().map { groups -> groups.map { it.toDomain() } }

    override fun observeArchivedGroups(): Flow<List<Group>> =
        groupDao.observeArchivedGroups().map { groups -> groups.map { it.toDomain() } }

    override fun observeGroup(groupId: String): Flow<Group?> = groupDao.observeGroup(groupId).map {
        it?.toDomain()
    }

    override suspend fun createGroup(name: String, guestNames: List<String>): String {
        val now = clock.millis()
        val user = currentUser.currentUser()
        val groupId = ids.newId()
        val group = GroupEntity(
            id = groupId,
            name = name.trim(),
            createdBy = user.userId,
            lastActivityAt = now,
            updatedAt = now,
        )
        // The creator joins first, then guests in the order they were added (member order, M1-Q2).
        val creator = MemberEntity(
            id = ids.newId(),
            groupId = groupId,
            displayName = user.displayName,
            userId = user.userId,
            createdAt = now,
            updatedAt = now,
        )
        val guests = guestNames.map {
            it.trim()
        }.filter { it.isNotEmpty() }.mapIndexed { index, guestName ->
            MemberEntity(
                id = ids.newId(),
                groupId = groupId,
                displayName = guestName,
                createdAt = now + index + 1,
                updatedAt = now,
            )
        }
        db.withTransaction {
            groupDao.upsert(group)
            db.memberDao().upsertAll(listOf(creator) + guests)
        }
        syncScheduler.requestSync()
        return groupId
    }

    override suspend fun renameGroup(groupId: String, name: String) {
        val now = clock.millis()
        db.withTransaction {
            groupDao.rename(groupId, name.trim(), updatedAt = now)
            groupDao.touchActivity(groupId, now)
        }
        syncScheduler.requestSync()
    }

    override suspend fun setArchived(groupId: String, isArchived: Boolean) {
        val now = clock.millis()
        db.withTransaction {
            groupDao.setArchived(groupId, isArchived, updatedAt = now)
            groupDao.touchActivity(groupId, now)
        }
        syncScheduler.requestSync()
    }
}
