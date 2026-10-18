package com.split.easy.sync.download

import com.split.easy.data.local.entity.SyncState

/**
 * Last write wins (PRD sync strategy): should a record from Firestore replace the one on this phone?
 *
 * - Not on this phone yet → take it.
 * - On this phone and already uploaded (SYNCED) → take it if it's at least as new; an equal
 *   time means it's the same edit coming back, which is harmless.
 * - Changed on this phone but not uploaded yet (PENDING) → take it only if it's strictly newer.
 *   Otherwise the local edit wins and will overwrite Firestore on the next upload.
 */
fun shouldTakeRemote(
    localUpdatedAt: Long?,
    localState: SyncState?,
    remoteUpdatedAt: Long,
): Boolean = when {
    localUpdatedAt == null -> true
    localState == SyncState.PENDING -> remoteUpdatedAt > localUpdatedAt
    else -> remoteUpdatedAt >= localUpdatedAt
}
