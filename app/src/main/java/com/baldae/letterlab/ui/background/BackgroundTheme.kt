package com.baldae.letterlab.ui.background

import com.baldae.letterlab.data.LevelCatalog
import com.baldae.letterlab.data.LevelProgress

/**
 * Unlockable background themes. All are rendered locally — animated gradients,
 * particles and geometry — so the game never needs the network.
 */
enum class BackgroundTheme(
    val id: String,
    val displayName: String,
    /** World that must be completed to unlock, or null if always available / [requiresAll]. */
    val unlockWorldId: Int?,
    val requiresAll: Boolean = false,
) {
    LABORATORY("laboratory", "Laboratory", unlockWorldId = null),
    AURORA("aurora", "Aurora", unlockWorldId = 1),
    GEOMETRY("geometry", "Geometry", unlockWorldId = 2),
    NEBULA("nebula", "Nebula", unlockWorldId = 3),
    EMBER("ember", "Ember", unlockWorldId = 4),
    GILDED("gilded", "Gilded", unlockWorldId = null, requiresAll = true);

    fun unlockDescription(catalog: LevelCatalog): String = when {
        requiresAll -> "Complete every level"
        unlockWorldId != null ->
            "Complete ${catalog.worlds.find { it.id == unlockWorldId }?.name ?: "world $unlockWorldId"}"
        else -> "Always unlocked"
    }

    companion object {
        fun fromId(id: String): BackgroundTheme =
            entries.find { it.id == id } ?: LABORATORY

        fun isUnlocked(
            theme: BackgroundTheme,
            catalog: LevelCatalog,
            progress: Map<Int, LevelProgress>,
        ): Boolean {
            val completed = progress.filterValues { it.completed }.keys
            return when {
                theme.requiresAll -> catalog.allLevels.all { it.id in completed }
                theme.unlockWorldId == null -> true
                else -> catalog.worlds.find { it.id == theme.unlockWorldId }
                    ?.levels?.all { it.id in completed } ?: false
            }
        }
    }
}
