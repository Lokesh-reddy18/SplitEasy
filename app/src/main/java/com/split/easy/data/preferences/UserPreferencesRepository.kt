package com.split.easy.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.split.easy.domain.model.ThemeSetting
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Small app settings that don't belong in Room or sync, stored with DataStore. */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
        // A corrupt or unreadable file falls back to the defaults instead of crashing.
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val themeSetting: Flow<ThemeSetting> = preferences.map { prefs ->
        prefs[THEME]?.let { name -> ThemeSetting.entries.firstOrNull { it.name == name } }
            ?: ThemeSetting.SYSTEM
    }

    suspend fun setThemeSetting(setting: ThemeSetting) {
        dataStore.edit { it[THEME] = setting.name }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
    }
}
