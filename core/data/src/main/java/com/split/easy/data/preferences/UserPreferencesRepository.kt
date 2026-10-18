package com.split.easy.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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

    /** The group an expense was last added to; listed first in the group picker (Q15). */
    val lastUsedGroupId: Flow<String?> = preferences.map { it[LAST_USED_GROUP] }

    suspend fun setLastUsedGroupId(groupId: String) {
        dataStore.edit { it[LAST_USED_GROUP] = groupId }
    }

    /** Groups where the first-time UPI ID prompt was dismissed with "Not now" (M2-Q7). */
    val dismissedUpiPromptGroups: Flow<Set<String>> = preferences.map {
        it[DISMISSED_UPI_PROMPT].orEmpty()
    }

    suspend fun dismissUpiPrompt(groupId: String) {
        dataStore.edit { it[DISMISSED_UPI_PROMPT] = it[DISMISSED_UPI_PROMPT].orEmpty() + groupId }
    }

    /** The account whose data is on this phone; null for none (M2 test data, or signed out). */
    val dataOwnerId: Flow<String?> = preferences.map { it[DATA_OWNER] }

    suspend fun setDataOwnerId(userId: String?) {
        dataStore.edit { if (userId == null) it.remove(DATA_OWNER) else it[DATA_OWNER] = userId }
    }

    /** Forgets per-account settings (last-used group, dismissed prompts). The theme stays (M4-Q2). */
    suspend fun clearAccountSettings() {
        dataStore.edit {
            it.remove(LAST_USED_GROUP)
            it.remove(DISMISSED_UPI_PROMPT)
        }
    }

    private companion object {
        val DATA_OWNER = stringPreferencesKey("data_owner")
        val DISMISSED_UPI_PROMPT = stringSetPreferencesKey("dismissed_upi_prompt")
        val THEME = stringPreferencesKey("theme")
        val LAST_USED_GROUP = stringPreferencesKey("last_used_group")
    }
}
