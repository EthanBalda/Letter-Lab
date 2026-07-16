package com.baldae.letterlab.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Stats(
    val totalMoves: Int = 0,
    val totalWins: Int = 0,
    val totalUndos: Int = 0,
)

class StatsRepository(private val dataStore: DataStore<Preferences>) {

    val stats: Flow<Stats> = dataStore.data.map { prefs ->
        Stats(
            totalMoves = prefs[MOVES] ?: 0,
            totalWins = prefs[WINS] ?: 0,
            totalUndos = prefs[UNDOS] ?: 0,
        )
    }

    suspend fun addMoves(count: Int) =
        dataStore.edit { it[MOVES] = (it[MOVES] ?: 0) + count }

    suspend fun addWin() =
        dataStore.edit { it[WINS] = (it[WINS] ?: 0) + 1 }

    suspend fun addUndo() =
        dataStore.edit { it[UNDOS] = (it[UNDOS] ?: 0) + 1 }

    private companion object {
        // stat_ prefix so ProgressRepository.resetAllProgress() wipes these too.
        val MOVES = intPreferencesKey("stat_moves")
        val WINS = intPreferencesKey("stat_wins")
        val UNDOS = intPreferencesKey("stat_undos")
    }
}
