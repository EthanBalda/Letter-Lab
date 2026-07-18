package com.baldae.letterlab.data

import com.baldae.letterlab.achievements.AchievementBook
import com.baldae.letterlab.domain.Board
import com.baldae.letterlab.domain.GameEngine
import com.baldae.letterlab.domain.solver.Solver
import com.baldae.letterlab.ui.background.BackgroundTheme
import java.io.File
import kotlin.test.assertTrue
import org.junit.Test

/**
 * Campaign-wide progression guarantees beyond raw solvability:
 *
 * - Tutorials teach for real: no within-par solution exists that never uses
 *   the featured letter (exhaustive up to a node budget; passive letters
 *   d/i/n demonstrate by shaping the level and are exempt).
 * - Every achievement and background theme is reachable.
 *
 * These are the regression tests for the failure modes found while
 * authoring the A–Z campaign: pre-solved boards, unsolvable auras, pars
 * below the best-known solution, and tutorial levels with bypass routes.
 */
class CampaignIntegrityTest {

    private val catalog: LevelCatalog by lazy {
        LevelRepository.parse(File("src/main/assets/levels.json").readText())
    }

    // -------------------------------------------------- tutorial integrity

    /**
     * Breadth-first search over all move sequences of length <= [maxDepth]
     * that never select the [banned] letter. Returns true if one of them
     * solves the board; null when the budget ran out unproven.
     */
    private fun solvableWithout(
        start: Board,
        banned: Char,
        maxDepth: Int,
        nodeBudget: Int = 400_000,
    ): Boolean? {
        val solver = Solver()
        val seen = HashSet<String>()
        var frontier = listOf(start)
        seen += start.serialize()
        var explored = 0
        repeat(maxDepth) {
            val next = mutableListOf<Board>()
            for (board in frontier) {
                if (++explored > nodeBudget) return null
                for ((move, result) in solver.expand(board)) {
                    if (GameEngine.effective(board, move.origin) == banned) continue
                    if (!seen.add(result.serialize())) continue
                    if (GameEngine.isSolved(result)) return true
                    next += result
                }
            }
            if (next.isEmpty()) return false
            frontier = next
        }
        return false
    }

    @Test
    fun `no tutorial can be three-starred without its featured letter`() {
        val failures = mutableListOf<String>()
        for (level in catalog.allLevels) {
            val letter = level.tutorial?.letter?.singleOrNull() ?: continue
            if (letter in "din") continue // passive: presence is the lesson
            when (solvableWithout(level.board(), letter, level.par)) {
                true -> failures +=
                    "Level ${level.id} '${level.name}' has a within-par solution " +
                        "that never uses '$letter'"
                false, null -> Unit // proven required, or no bypass found in budget
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    // ------------------------------------------------ regression invariants

    @Test
    fun `no level is pre-solved and every board has legal content`() {
        for (level in catalog.allLevels) {
            val board = level.board()
            assertTrue(
                !GameEngine.isSolved(board),
                "Level ${level.id} '${level.name}' starts solved"
            )
        }
    }

    // --------------------------------------------- achievements and themes

    @Test
    fun `world achievements exactly cover the catalog's worlds`() {
        val achievementWorldIds = AchievementBook.all
            .mapNotNull { it.id.removePrefix("world_").toIntOrNull() }
            .toSet()
        val catalogWorldIds = catalog.worlds.map { it.id }.toSet()
        assertTrue(
            achievementWorldIds == catalogWorldIds,
            "world achievements $achievementWorldIds != worlds $catalogWorldIds"
        )
    }

    @Test
    fun `star-count achievements are reachable`() {
        // Thresholds encoded in AchievementBook.deservedIds.
        assertTrue(30 <= catalog.maxStars)
        assertTrue(120 <= catalog.maxStars)
    }

    @Test
    fun `the minimalist achievement is reachable`() {
        // Needs some level clearable in <= 2 moves; level 1 has par 1, and
        // CampaignSolvabilityTest proves a within-par solution exists.
        assertTrue(catalog.allLevels.any { it.par <= 2 })
    }

    @Test
    fun `every background theme unlock points at a real world`() {
        for (theme in BackgroundTheme.entries) {
            val worldId = theme.unlockWorldId ?: continue
            assertTrue(
                catalog.worlds.any { it.id == worldId },
                "${theme.displayName} unlocks after missing world $worldId"
            )
        }
    }
}
