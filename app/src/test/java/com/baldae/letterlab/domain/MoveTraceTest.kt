package com.baldae.letterlab.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

/**
 * Movement traces drive the tile animations: every travelling letter must be
 * reported exactly once, with the content it carried and whether it survived.
 */
class MoveTraceTest {

    private fun board(text: String) = Board.parse(text.trimIndent())

    private fun trace(board: Board, origin: Pos, target: Pos? = null): MoveResult =
        checkNotNull(GameEngine.applyWithTrace(board, origin, target))

    @Test
    fun `swap reports both letters crossing`() {
        val result = trace(board("ab"), Pos(0, 1), Pos(0, 0))
        assertEquals(
            setOf(
                TileMove(Pos(0, 1), Pos(0, 0), Cell('b')),
                TileMove(Pos(0, 0), Pos(0, 1), Cell('a')),
            ),
            result.tileMoves.toSet(),
        )
    }

    @Test
    fun `swapping into a blank reports a single journey`() {
        val result = trace(board("b_"), Pos(0, 0), Pos(0, 1))
        assertEquals(listOf(TileMove(Pos(0, 0), Pos(0, 1), Cell('b'))), result.tileMoves)
    }

    @Test
    fun `swapping into a d reports a vanishing journey`() {
        val result = trace(board("bd"), Pos(0, 0), Pos(0, 1))
        assertEquals(
            listOf(TileMove(Pos(0, 0), Pos(0, 1), Cell('b'), vanishes = true)),
            result.tileMoves,
        )
    }

    @Test
    fun `eat pulls the meal onto the eater`() {
        val result = trace(board("ec"), Pos(0, 0), Pos(0, 1))
        assertEquals(listOf(TileMove(Pos(0, 1), Pos(0, 0), Cell('c'))), result.tileMoves)
    }

    @Test
    fun `eating a blank perishes in place`() {
        val result = trace(board("e_"), Pos(0, 0), Pos(0, 1))
        assertEquals(
            listOf(TileMove(Pos(0, 0), Pos(0, 0), Cell('e'), vanishes = true)),
            result.tileMoves,
        )
    }

    @Test
    fun `force reports every travelling letter including the wrap`() {
        val result = trace(board("fab"), Pos(0, 0), Pos(0, 1)) // shift right
        assertEquals(
            setOf(
                TileMove(Pos(0, 0), Pos(0, 1), Cell('f')),
                TileMove(Pos(0, 1), Pos(0, 2), Cell('a')),
                TileMove(Pos(0, 2), Pos(0, 0), Cell('b')), // wraps to the front
            ),
            result.tileMoves.toSet(),
        )
    }

    @Test
    fun `a letter shifted into a d vanishes at the d's cell`() {
        // "d b c f b" shift right: the trailing b wraps into the d and dies.
        val result = trace(board("dbcfb"), Pos(0, 3), Pos(0, 4))
        val vanish = result.tileMoves.single { it.vanishes }
        assertEquals(TileMove(Pos(0, 4), Pos(0, 0), Cell('b'), vanishes = true), vanish)
        // The d donates a blank: nothing claims to move into (0,1).
        assertTrue(result.tileMoves.none { it.to == Pos(0, 1) })
    }

    @Test
    fun `grab reports the long pull and the backfill shift`() {
        val result = trace(board("abcag"), Pos(0, 4), Pos(0, 0))
        assertEquals(
            setOf(
                TileMove(Pos(0, 0), Pos(0, 3), Cell('a')), // grabbed letter flies in
                TileMove(Pos(0, 3), Pos(0, 2), Cell('a')),
                TileMove(Pos(0, 2), Pos(0, 1), Cell('c')),
                TileMove(Pos(0, 1), Pos(0, 0), Cell('b')),
            ),
            result.tileMoves.toSet(),
        )
    }

    @Test
    fun `grabbing a d reports its journey and the wiped victims`() {
        val result = trace(board("gabd"), Pos(0, 0), Pos(0, 3))
        val journey = result.tileMoves.single { !it.vanishes }
        assertEquals(TileMove(Pos(0, 3), Pos(0, 1), Cell('d')), journey)
        val victims = result.tileMoves.filter { it.vanishes }.map { it.from }.toSet()
        assertEquals(setOf(Pos(0, 1), Pos(0, 2)), victims)
    }

    @Test
    fun `hold reports the lift and the drop`() {
        val start = board("ahb").with(Pos(0, 1) to Cell('h', 'c'))
        val result = trace(start, Pos(0, 1), Pos(0, 2)) // swap held c with the b
        assertEquals(
            setOf(
                TileMove(Pos(0, 2), Pos(0, 1), Cell('b')), // lifted
                TileMove(Pos(0, 1), Pos(0, 2), Cell('c')), // dropped
            ),
            result.tileMoves.toSet(),
        )
    }

    @Test
    fun `lovely reports moves from every direction it kicks`() {
        val result = trace(board("bacblccbb"), Pos(0, 4))
        // Both sides of the l shift; nothing crosses the l itself.
        assertTrue(result.tileMoves.isNotEmpty())
        assertTrue(result.tileMoves.none { it.from == Pos(0, 4) || it.to == Pos(0, 4) })
    }

    @Test
    fun `traces never move empty cells`() {
        val boards = listOf("f_ab", "g__b", "l_b_a")
        for (text in boards) {
            val b = board(text)
            val origin = Pos(0, 0)
            val result = GameEngine.applyWithTrace(
                b, origin,
                if (b[origin].letter in Letters.IMMEDIATE) null else Pos(0, 1),
            ) ?: continue
            assertTrue(result.tileMoves.none { it.cell.isEmpty }, "empty tile moved in $text")
        }
    }

    @Test
    fun `every trace destination matches the resulting board`() {
        // For surviving moves, the letter recorded must sit at its destination.
        val cases = listOf(
            Triple("abcag", Pos(0, 4), Pos(0, 0)),
            Triple("bkabba", Pos(0, 1), Pos(0, 2)),
            Triple("fab", Pos(0, 0), Pos(0, 1)),
            Triple("aacaa", Pos(0, 2), Pos(0, 0)),
        )
        for ((text, origin, target) in cases) {
            val result = trace(board(text), origin, target)
            for (move in result.tileMoves.filter { !it.vanishes }) {
                assertEquals(
                    move.cell, result.board[move.to],
                    "In $text, ${move.cell} should have landed at ${move.to}",
                )
            }
        }
    }
}
