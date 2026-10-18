package com.split.easy.domain.sync

/** Asks for pending changes to be uploaded. Repositories call it after every local write. */
fun interface SyncScheduler {
    fun requestSync()
}
