package com.split.easy.data.local

import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Deletes every group, member, expense, settlement and event on this phone. */
fun interface LocalDataEraser {
    suspend fun eraseAll()
}

class RoomDataEraser @Inject constructor(private val database: SplitEasyDatabase) :
    LocalDataEraser {
    override suspend fun eraseAll() = withContext(Dispatchers.IO) { database.clearAllTables() }
}
