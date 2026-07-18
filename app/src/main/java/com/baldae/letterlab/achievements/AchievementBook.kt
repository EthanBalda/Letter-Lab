package com.baldae.letterlab.achievements

import com.baldae.letterlab.data.LevelCatalog
import com.baldae.letterlab.data.LevelProgress
import com.baldae.letterlab.data.Stats

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
)

/**
 * Every achievement is a pure function of persisted state, so [deservedIds]
 * can simply be re-evaluated after any win or undo; the repository diffs the
 * result against what's already unlocked.
 */
object AchievementBook {

    val all: List<Achievement> = listOf(
        Achievement("first_win", "First Result", "Complete your first level", "🧪"),
        Achievement("world_1", "Solid Foundations", "Complete every level in Foundations", "🔬"),
        Achievement("world_2", "Chain Reactor", "Complete every level in Reactions", "⚗️"),
        Achievement("world_3", "Kineticist", "Complete every level in Kinetics", "🧲"),
        Achievement("world_4", "Dynamo", "Complete every level in Dynamics", "⚡"),
        Achievement("world_5", "Synthesist", "Complete every level in Synthesis", "🧬"),
        Achievement("world_6", "Sound of Silence", "Complete every level in Silence", "🤫"),
        Achievement("world_7", "Wrecking Ball", "Complete every level in Demolition Crew", "💥"),
        Achievement("world_8", "Transmuter", "Complete every level in Transmutation", "⚗️"),
        Achievement("world_9", "Choreographer", "Complete every level in Choreography", "🩰"),
        Achievement("world_10", "Ascendant", "Complete every level in Ascension", "👑"),
        Achievement("all_clear", "Graduate", "Complete every level in the game", "🎓"),
        Achievement("efficient", "Efficient", "Clear any level within par", "🎯"),
        Achievement("minimalist", "Minimalist", "Clear any level in 2 moves or fewer", "✨"),
        Achievement("stars_30", "Star Collector", "Earn 30 stars", "⭐"),
        Achievement("stars_120", "Constellation", "Earn 120 stars", "💫"),
        Achievement("stars_75", "Perfectionist", "Earn every star in the game", "🌟"),
        Achievement("wins_10", "Lab Regular", "Solve levels 10 times", "🥼"),
        Achievement("moves_100", "Century", "Make 100 moves", "💯"),
        Achievement("moves_500", "Marathon", "Make 500 moves", "🏃"),
        Achievement("undo_25", "Time Traveler", "Undo 25 moves", "⏪"),
    )

    fun byId(id: String): Achievement? = all.find { it.id == id }

    fun deservedIds(
        catalog: LevelCatalog,
        progress: Map<Int, LevelProgress>,
        stats: Stats,
    ): Set<String> {
        val deserved = mutableSetOf<String>()
        val completed = progress.filterValues { it.completed }.keys
        val totalStars = catalog.allLevels.sumOf { progress[it.id]?.stars ?: 0 }

        if (completed.isNotEmpty()) deserved += "first_win"
        for (world in catalog.worlds) {
            if (world.levels.all { it.id in completed }) deserved += "world_${world.id}"
        }
        if (catalog.allLevels.all { it.id in completed }) deserved += "all_clear"
        if (progress.values.any { it.stars == 3 }) deserved += "efficient"
        if (progress.values.any { (it.bestMoves ?: Int.MAX_VALUE) <= 2 }) deserved += "minimalist"
        if (totalStars >= 30) deserved += "stars_30"
        if (totalStars >= 120) deserved += "stars_120"
        if (totalStars >= catalog.maxStars) deserved += "stars_75"
        if (stats.totalWins >= 10) deserved += "wins_10"
        if (stats.totalMoves >= 100) deserved += "moves_100"
        if (stats.totalMoves >= 500) deserved += "moves_500"
        if (stats.totalUndos >= 25) deserved += "undo_25"
        return deserved
    }
}
