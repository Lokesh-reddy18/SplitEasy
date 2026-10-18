package com.split.easy.data.user

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.split.easy.data.preferences.UserPreferencesRepository
import com.split.easy.domain.model.CurrentUser
import com.split.easy.domain.model.Session
import com.split.easy.domain.model.ThemeSetting
import com.split.easy.domain.repository.SyncStatusRepository
import com.split.easy.testing.FakeAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SessionManagerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val ananya = CurrentUser("uid-ananya", "Ananya Rao")
    private val ravi = CurrentUser("uid-ravi", "Ravi Kumar")

    private val auth = FakeAuthRepository()
    private var wipes = 0
    private val pending = MutableStateFlow(0)
    private val syncStatus = object : SyncStatusRepository {
        override fun observePendingChangeCount() = pending
    }

    private fun TestScope.setUp(): Pair<SessionManager, UserPreferencesRepository> {
        val file = folder.newFile("settings.preferences_pb").also { it.delete() }
        val preferences =
            UserPreferencesRepository(
                PreferenceDataStoreFactory.create(scope = backgroundScope) {
                    file
                },
            )
        return SessionManager(auth, { wipes++ }, preferences, syncStatus) to preferences
    }

    @Test
    fun `signed out shows the sign-in screen`() = runTest {
        val (session) = setUp()
        assertEquals(Session.SignedOut, session.session.first())
        assertEquals(0, wipes)
    }

    @Test
    fun `first sign-in wipes the M2 test data and per-account settings, but keeps the theme`() =
        runTest {
            val (session, preferences) = setUp()
            preferences.setThemeSetting(ThemeSetting.DARK)
            preferences.setLastUsedGroupId("old-group")
            preferences.dismissUpiPrompt("old-group")

            session.session.test {
                assertEquals(Session.SignedOut, awaitItem())
                auth.signIn(ananya)
                assertEquals(Session.Preparing(ananya), awaitItem())
                assertEquals(Session.SignedIn(ananya), awaitItem())
            }
            assertEquals(1, wipes)
            assertEquals("uid-ananya", preferences.dataOwnerId.first())
            assertNull(preferences.lastUsedGroupId.first())
            assertEquals(emptySet<String>(), preferences.dismissedUpiPromptGroups.first())
            assertEquals(ThemeSetting.DARK, preferences.themeSetting.first())
        }

    @Test
    fun `opening the app again as the same account keeps the data`() = runTest {
        val (session, preferences) = setUp()
        preferences.setDataOwnerId("uid-ananya")
        auth.signIn(ananya)
        assertEquals(Session.SignedIn(ananya), session.session.first())
        assertEquals(0, wipes)
    }

    @Test
    fun `a different account never sees the previous account's data`() = runTest {
        val (session, preferences) = setUp()
        preferences.setDataOwnerId("uid-ananya")
        auth.signIn(ravi)
        session.session.test {
            assertEquals(Session.Preparing(ravi), awaitItem())
            assertEquals(Session.SignedIn(ravi), awaitItem())
        }
        assertEquals(1, wipes)
        assertEquals("uid-ravi", preferences.dataOwnerId.first())
    }

    @Test
    fun `sign out deletes local data and reports unsynced changes first`() = runTest {
        val (session, preferences) = setUp()
        preferences.setDataOwnerId("uid-ananya")
        auth.signIn(ananya)
        pending.value = 3
        assertEquals(3, session.unsyncedChangeCount())

        session.signOut()
        assertEquals(1, wipes)
        assertNull(preferences.dataOwnerId.first())
        assertEquals(Session.SignedOut, session.session.first())
    }
}
