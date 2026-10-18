package com.split.easy.domain.repository

import kotlinx.coroutines.flow.Flow

interface SyncStatusRepository {
    /** Local changes not uploaded yet ("2 changes waiting to sync"). */
    fun observePendingChangeCount(): Flow<Int>
}
