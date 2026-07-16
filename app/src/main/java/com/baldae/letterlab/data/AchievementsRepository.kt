package com.baldae.letterlab.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AchievementsRepository(private val dataStore: DataStore<Preferences>) {

    val unlocked: Flow<Set<String>> = dataStore.data.map { it[UNLOCKED] ?: emptySet() }

    /** Unlocks [ids]; returns the ones that were not already unlocked. */
    suspend fun unlock(ids: Set<String>): Set<String> {
        var newlyUnlocked: Set<String> = emptySet()
        dataStore.edit { prefs ->
            val current = prefs[UNLOCKED] ?: emptySet()
            newlyUnlocked = ids - current
            if (newlyUnlocked.isNotEmpty()) prefs[UNLOCKED] = current + ids
        }
        return newlyUnlocked
    }

    private companion object {
        // ach_ prefix so ProgressRepository.resetAllProgress() wipes it.
        val UNLOCKED = stringSetPreferencesKey("ach_unlocked")
    }
}
