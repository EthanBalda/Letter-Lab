package com.baldae.letterlab.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val themeId: String = "laboratory",
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<Settings> = dataStore.data.map { prefs ->
        Settings(
            soundEnabled = prefs[SOUND] ?: true,
            musicEnabled = prefs[MUSIC] ?: true,
            hapticsEnabled = prefs[HAPTICS] ?: true,
            themeId = prefs[THEME] ?: "laboratory",
        )
    }

    suspend fun setSoundEnabled(enabled: Boolean) =
        dataStore.edit { it[SOUND] = enabled }

    suspend fun setMusicEnabled(enabled: Boolean) =
        dataStore.edit { it[MUSIC] = enabled }

    suspend fun setHapticsEnabled(enabled: Boolean) =
        dataStore.edit { it[HAPTICS] = enabled }

    suspend fun setTheme(themeId: String) =
        dataStore.edit { it[THEME] = themeId }

    private companion object {
        val SOUND = booleanPreferencesKey("settings_sound")
        val MUSIC = booleanPreferencesKey("settings_music")
        val HAPTICS = booleanPreferencesKey("settings_haptics")
        val THEME = stringPreferencesKey("settings_theme")
    }
}
