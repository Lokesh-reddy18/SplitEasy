package com.lokeshreddy.spliteasy.data.local.entity

/** Every local write is PENDING until the sync worker uploads it, then SYNCED. */
enum class SyncState {
    PENDING,
    SYNCED,
}
