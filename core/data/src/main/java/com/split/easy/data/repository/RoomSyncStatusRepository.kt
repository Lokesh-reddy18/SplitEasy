package com.split.easy.data.repository

import com.split.easy.data.local.dao.SyncStatusDao
import com.split.easy.domain.repository.SyncStatusRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class RoomSyncStatusRepository @Inject constructor(private val syncStatusDao: SyncStatusDao) :
    SyncStatusRepository {
    override fun observePendingChangeCount(): Flow<Int> = syncStatusDao.observePendingChangeCount()
}
