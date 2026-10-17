package com.split.easy.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SplitEasyDatabase::class.java,
    )

    @Test
    fun `v1 to v2 adds lastActivityAt, starting from each group's updatedAt`() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            db.execSQL(
                "INSERT INTO groups " +
                    "(id, name, createdBy, isArchived, updatedAt, syncState, isDeleted) " +
                    "VALUES ('g1', 'Coorg trip', 'uid-you', 0, 1234, 'SYNCED', 0)",
            )
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)
        migrated.use { db ->
            val query = "SELECT lastActivityAt, updatedAt, syncState FROM groups WHERE id = 'g1'"
            val cursor = db.query(query)
            cursor.use {
                it.moveToFirst()
                assertEquals(1234L, it.getLong(0))
                assertEquals(1234L, it.getLong(1))
                assertEquals("SYNCED", it.getString(2))
            }
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
