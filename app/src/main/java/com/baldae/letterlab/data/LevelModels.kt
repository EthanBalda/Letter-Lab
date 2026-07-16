package com.baldae.letterlab.data

import com.baldae.letterlab.domain.Board
import kotlinx.serialization.Serializable

@Serializable
data class TutorialDef(
    /** Letter this tutorial introduces, e.g. "b", or null for general tips. */
    val letter: String? = null,
    val title: String,
    val body: String,
)

@Serializable
data class LevelDef(
    val id: Int,
    val name: String,
    /** Move count for a 3-star clear. Tunable data, not a solver guarantee. */
    val par: Int,
    val grid: List<String>,
    val tutorial: TutorialDef? = null,
) {
    fun board(): Board = Board.fromRows(grid)

    fun starsFor(moves: Int): Int = when {
        moves <= par -> 3
        moves <= par * 2 -> 2
        else -> 1
    }
}

@Serializable
data class WorldDef(
    val id: Int,
    val name: String,
    val levels: List<LevelDef>,
)

@Serializable
data class LevelCatalog(val worlds: List<WorldDef>) {
    val allLevels: List<LevelDef> by lazy { worlds.flatMap { it.levels } }

    fun level(id: Int): LevelDef? = allLevels.find { it.id == id }

    fun worldOf(levelId: Int): WorldDef? =
        worlds.find { world -> world.levels.any { it.id == levelId } }

    /** The level after [levelId] in play order, or null if it was the last. */
    fun nextLevel(levelId: Int): LevelDef? {
        val idx = allLevels.indexOfFirst { it.id == levelId }
        return if (idx >= 0) allLevels.getOrNull(idx + 1) else null
    }

    val maxStars: Int get() = allLevels.size * 3
}
