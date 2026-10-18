package com.split.easy.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Gives each test a fresh in-memory database, run on the JVM by Robolectric. */
@RunWith(RobolectricTestRunner::class)
abstract class DatabaseTest {

    protected lateinit var db: SplitEasyDatabase

    @Before
    fun createDatabase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SplitEasyDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        db.close()
    }
}
