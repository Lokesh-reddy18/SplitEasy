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
