package com.split.easy.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.split.easy.domain.model.ThemeSetting
import java.io.File
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserPreferencesRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.repository(
        file: File = folder.newFile("settings.preferences_pb").also {
            it.delete()
        },
    ) = UserPreferencesRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { file },
    )

    @Test
    fun `theme defaults to system and remembers the choice`() = runTest {
        val repository = repository()
        repository.themeSetting.test {
            assertEquals(ThemeSetting.SYSTEM, awaitItem())
            repository.setThemeSetting(ThemeSetting.DARK)
            assertEquals(ThemeSetting.DARK, awaitItem())
            repository.setThemeSetting(ThemeSetting.LIGHT)
            assertEquals(ThemeSetting.LIGHT, awaitItem())
        }
    }
}
