package com.split.easy.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v2 adds groups.lastActivityAt (M2-Q4). Existing groups start from their last update. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `groups` ADD COLUMN `lastActivityAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `groups` SET `lastActivityAt` = `updatedAt`")
    }
}

/** M4: remembers when each group was last downloaded from Firestore. Starts empty. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sync_progress` " +
                "(`groupId` TEXT NOT NULL, `lastDownloadedAt` INTEGER NOT NULL, PRIMARY KEY(`groupId`))",
        )
    }
}
