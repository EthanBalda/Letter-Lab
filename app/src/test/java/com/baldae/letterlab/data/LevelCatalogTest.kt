package com.baldae.letterlab.data

import com.baldae.letterlab.domain.GameEngine
import com.baldae.letterlab.domain.Letters
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

/** Validates the shipped levels.json asset against the engine's rules. */
class LevelCatalogTest {

    private val catalog: LevelCatalog by lazy {
        LevelRepository.parse(File("src/main/assets/levels.json").readText())
    }

    @Test
    fun `catalog has five worlds and twenty-five levels`() {
        assertEquals(5, catalog.worlds.size)
        assertEquals(25, catalog.allLevels.size)
    }

    @Test
    fun `level ids are unique and sequential`() {
        assertEquals((1..25).toList(), catalog.allLevels.map { it.id })
    }

    @Test
    fun `every grid parses into a valid rectangular board`() {
        for (level in catalog.allLevels) {
            val board = level.board() // throws on bad letters / ragged rows
            assertTrue(board.rows > 0 && board.cols > 0, "Level ${level.id} is empty")
        }
    }

    @Test
    fun `no level starts already solved`() {
        for (level in catalog.allLevels) {
            assertFalse(
                GameEngine.isSolved(level.board()),
                "Level ${level.id} '${level.name}' is solved before the first move"
            )
        }
    }

    @Test
    fun `every level contains at least one usable action letter`() {
        for (level in catalog.allLevels) {
            val board = level.board()
            assertTrue(
                board.positions().any { board[it].letter in Letters.SELECTABLE },
                "Level ${level.id} has no selectable letters"
            )
        }
    }

    @Test
    fun `pars are positive and stars follow the par rule`() {
        for (level in catalog.allLevels) {
            assertTrue(level.par > 0, "Level ${level.id} has no par")
            assertEquals(3, level.starsFor(level.par))
            assertEquals(2, level.starsFor(level.par * 2))
            assertEquals(1, level.starsFor(level.par * 2 + 1))
        }
    }

    @Test
    fun `tutorial letters actually appear in their level`() {
        for (level in catalog.allLevels) {
            val letter = level.tutorial?.letter?.singleOrNull() ?: continue
            assertTrue(
                level.grid.any { row -> letter in row },
                "Level ${level.id} introduces '$letter' but does not contain it"
            )
        }
    }

    @Test
    fun `next level ordering follows catalog order`() {
        assertEquals(2, catalog.nextLevel(1)?.id)
        assertEquals(6, catalog.nextLevel(5)?.id)
        assertEquals(null, catalog.nextLevel(25))
    }
}
