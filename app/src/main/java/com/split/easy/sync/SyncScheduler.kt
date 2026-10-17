package com.split.easy.sync

/** Asks for pending changes to be uploaded. Repositories call it after every local write. */
interface SyncScheduler {
    fun requestSync()
}

/** M2 has no backend yet, so this does nothing. M4 replaces it with a WorkManager job (FR-10). */
class NoOpSyncScheduler : SyncScheduler {
    override fun requestSync() = Unit
}
