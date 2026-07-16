package com.baldae.letterlab.ui.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baldae.letterlab.AppContainer
import com.baldae.letterlab.data.LevelDef
import com.baldae.letterlab.data.WorldDef
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LevelItem(
    val def: LevelDef,
    val stars: Int,
    val bestMoves: Int?,
    val completed: Boolean,
    val locked: Boolean,
    /** True when a mid-level snapshot exists to resume. */
    val inProgress: Boolean,
)

data class WorldSection(
    val world: WorldDef,
    val items: List<LevelItem>,
) {
    val completedCount: Int get() = items.count { it.completed }
}

data class LevelSelectUiState(
    val worlds: List<WorldSection> = emptyList(),
    val totalStars: Int = 0,
    val maxStars: Int = 0,
)

class LevelSelectViewModel(container: AppContainer) : ViewModel() {

    private val catalog = container.levelRepository.catalog

    val state: StateFlow<LevelSelectUiState> = combine(
        container.progressRepository.progress,
        container.progressRepository.savedLevelIds,
    ) { progress, savedIds ->
        // A level is playable once the previous level (in catalog order)
        // has been completed.
        val order = catalog.allLevels
        val unlocked = mutableSetOf<Int>()
        for ((index, level) in order.withIndex()) {
            val prevDone = index == 0 ||
                progress[order[index - 1].id]?.completed == true
            if (prevDone) unlocked += level.id
        }
        LevelSelectUiState(
            worlds = catalog.worlds.map { world ->
                WorldSection(
                    world = world,
                    items = world.levels.map { level ->
                        val p = progress[level.id]
                        LevelItem(
                            def = level,
                            stars = p?.stars ?: 0,
                            bestMoves = p?.bestMoves,
                            completed = p?.completed == true,
                            locked = level.id !in unlocked,
                            inProgress = level.id in savedIds,
                        )
                    },
                )
            },
            totalStars = catalog.allLevels.sumOf { progress[it.id]?.stars ?: 0 },
            maxStars = catalog.maxStars,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LevelSelectUiState())

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LevelSelectViewModel(container) as T
            }
    }
}
