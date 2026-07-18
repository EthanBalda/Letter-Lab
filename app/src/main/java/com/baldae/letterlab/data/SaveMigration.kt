package com.baldae.letterlab.data

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.baldae.letterlab.domain.Board

/**
 * Versioned save migration, run by DataStore before the first read.
 *
 * Version 1 was the 25-level campaign. Version 2 (the A–Z expansion)
 * renumbered every level, so all level-keyed entries — completion, stars,
 * best moves, mid-level snapshots, seen tutorials — are remapped to their
 * new ids. The mapping is conservative:
 *
 * - Completion and stars carry over: the player earned them on the very
 *   same grids. Nothing is marked complete that wasn't.
 * - Best moves and resume snapshots carry over only where the grid is
 *   byte-identical (all old levels except [REDESIGNED_OLD_IDS]); a best
 *   count for a redesigned board would be meaningless or unbeatable.
 * - Entries that don't parse, don't map, or don't cast cleanly are
 *   dropped rather than crashing or corrupting neighbours.
 * - Stats, achievements, and settings are level-independent and copied
 *   verbatim.
 */
class SaveMigration : DataMigration<Preferences> {

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        (currentData[SAVE_VERSION] ?: 1) < CURRENT_VERSION

    override suspend fun migrate(currentData: Preferences): Preferences =
        migrateToV2(currentData)

    override suspend fun cleanUp() = Unit

    companion object {
        val SAVE_VERSION: Preferences.Key<Int> = intPreferencesKey("save_version")
        const val CURRENT_VERSION = 2

        /** v1 campaign level id -> v2 campaign level id. */
        val OLD_TO_NEW: Map<Int, Int> = mapOf(
            1 to 1,   // First Swap
            2 to 2,   // Double Take
            3 to 3,   // Going Long
            4 to 4,   // The Cross
            5 to 6,   // Handle With Care
            6 to 8,   // Concentric
            7 to 9,   // The Hungry One (grid redesigned)
            8 to 10,  // Balanced Diet
            9 to 12,  // Full Force
            10 to 14, // Chain Reaction
            11 to 17, // Grab and Go
            12 to 18, // Corner Pockets
            13 to 23, // Hold That Thought -> Heavy Lifting
            14 to 21, // Juggling Act
            15 to 22, // Leapfrog
            16 to 30, // Acid Bath
            17 to 29, // Kickoff -> Counterattack
            18 to 26, // Set Pieces
            19 to 28, // Love Letters
            20 to 31, // Assembly Line
            21 to 33, // The Monster
            22 to 34, // Monster Mash
            23 to 32, // Demolition
            24 to 36, // The Gauntlet
            25 to 37, // Final Exam -> Midterm Exam
        )

        /** Old ids whose grid changed in v2: best moves and snapshots don't carry. */
        val REDESIGNED_OLD_IDS: Set<Int> = setOf(7)

        private val LEVEL_PREFIXES = listOf("done_", "best_", "stars_", "save_", "tut_")

        internal fun migrateToV2(old: Preferences): Preferences {
            val out = mutablePreferencesOf()
            for ((key, value) in old.asMap()) {
                val name = key.name
                val prefix = LEVEL_PREFIXES.firstOrNull { name.startsWith(it) }
                val oldId = prefix?.let { name.removePrefix(it).toIntOrNull() }
                if (prefix == null || oldId == null) {
                    // Stats, achievements, settings: level-independent, keep.
                    copyVerbatim(out, key, value)
                    continue
                }
                val newId = OLD_TO_NEW[oldId] ?: continue // unknown level entry: drop
                // Corrupt or mistyped entries are dropped, never fatal.
                runCatching {
                    when (prefix) {
                        "done_", "tut_" ->
                            out[booleanPreferencesKey(prefix + newId)] = value as Boolean
                        "stars_" ->
                            out[intPreferencesKey(prefix + newId)] = value as Int
                        "best_" ->
                            if (oldId !in REDESIGNED_OLD_IDS) {
                                out[intPreferencesKey(prefix + newId)] = value as Int
                            }
                        "save_" ->
                            if (oldId !in REDESIGNED_OLD_IDS && isValidSnapshot(value)) {
                                out[stringPreferencesKey(prefix + newId)] = value as String
                            }
                    }
                }
            }
            out[SAVE_VERSION] = CURRENT_VERSION
            return out.toPreferences()
        }

        /** A resumable snapshot must still parse into a legal board. */
        private fun isValidSnapshot(value: Any?): Boolean = runCatching {
            val raw = value as String
            val parts = raw.split("|", limit = 2)
            require(parts.size == 2)
            val board = Board.deserialize(parts[0])
            val cellsLegal = board.positions().all { pos ->
                val cell = board[pos]
                com.baldae.letterlab.domain.Letters.isValidCell(cell.letter) &&
                    (cell.held == null || cell.held in com.baldae.letterlab.domain.Letters.ALL)
            }
            cellsLegal && parts[1].toInt() >= 0
        }.getOrDefault(false)

        @Suppress("UNCHECKED_CAST")
        private fun copyVerbatim(
            out: androidx.datastore.preferences.core.MutablePreferences,
            key: Preferences.Key<*>,
            value: Any,
        ) {
            out[key as Preferences.Key<Any>] = value
        }
    }
}
