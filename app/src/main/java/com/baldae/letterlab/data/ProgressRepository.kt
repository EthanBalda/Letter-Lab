package com.baldae.letterlab.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.baldae.letterlab.domain.Board
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class LevelProgress(
    val completed: Boolean = false,
    val bestMoves: Int? = null,
    val stars: Int = 0,
)

/** A resumable mid-level snapshot. */
data class SavedGame(val board: Board, val moves: Int)

/**
 * Per-level progression: completion, best move counts, star ratings,
 * mid-level resume snapshots and which tutorials have been seen.
 */
class ProgressRepository(private val dataStore: DataStore<Preferences>) {

    val progress: Flow<Map<Int, LevelProgress>> = dataStore.data.map { prefs ->
        val result = mutableMapOf<Int, LevelProgress>()
        for ((key, value) in prefs.asMap()) {
            val name = key.name
            val id = name.substringAfter('_').toIntOrNull() ?: continue
            val current = result[id] ?: LevelProgress()
            result[id] = when {
                name.startsWith("done_") -> current.copy(completed = value as Boolean)
                name.startsWith("best_") -> current.copy(bestMoves = value as Int)
                name.startsWith("stars_") -> current.copy(stars = value as Int)
                else -> continue
            }
        }
        result
    }

    val seenTutorials: Flow<Set<Int>> = dataStore.data.map { prefs ->
        prefs.asMap().keys
            .filter { it.name.startsWith("tut_") }
            .mapNotNull { it.name.substringAfter('_').toIntOrNull() }
            .toSet()
    }

    /** Levels with a resumable mid-level snapshot. */
    val savedLevelIds: Flow<Set<Int>> = dataStore.data.map { prefs ->
        prefs.asMap().keys
            .filter { it.name.startsWith("save_") }
            .mapNotNull { it.name.substringAfter('_').toIntOrNull() }
            .toSet()
    }

    suspend fun recordWin(levelId: Int, moves: Int, stars: Int) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey("done_$levelId")] = true
            val bestKey = intPreferencesKey("best_$levelId")
            prefs[bestKey] = minOf(prefs[bestKey] ?: Int.MAX_VALUE, moves)
            val starsKey = intPreferencesKey("stars_$levelId")
            prefs[starsKey] = maxOf(prefs[starsKey] ?: 0, stars)
            prefs.remove(stringPreferencesKey("save_$levelId"))
        }
    }

    suspend fun saveInProgress(levelId: Int, board: Board, moves: Int) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("save_$levelId")] = "${board.serialize()}|$moves"
        }
    }

    suspend fun clearInProgress(levelId: Int) {
        dataStore.edit { prefs -> prefs.remove(stringPreferencesKey("save_$levelId")) }
    }

    suspend fun loadInProgress(levelId: Int): SavedGame? {
        val raw = dataStore.data.first()[stringPreferencesKey("save_$levelId")] ?: return null
        return runCatching {
            val (boardData, moves) = raw.split("|", limit = 2)
            SavedGame(Board.deserialize(boardData), moves.toInt())
        }.getOrNull()
    }

    suspend fun markTutorialSeen(levelId: Int) {
        dataStore.edit { prefs -> prefs[booleanPreferencesKey("tut_$levelId")] = true }
    }

    /** Wipes progression, stats and achievements — but not settings. */
    suspend fun resetAllProgress() {
        dataStore.edit { prefs ->
            val doomed = prefs.asMap().keys.filter { key ->
                PROGRESS_PREFIXES.any { key.name.startsWith(it) }
            }
            doomed.forEach { prefs.remove(it) }
        }
    }

    private companion object {
        val PROGRESS_PREFIXES =
            listOf("done_", "best_", "stars_", "save_", "tut_", "stat_", "ach_")
    }
}
