package com.baldae.letterlab.domain.solver

import com.baldae.letterlab.domain.Board
import com.baldae.letterlab.domain.GameEngine
import com.baldae.letterlab.domain.Pos
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test

class SolverTest {

    private val solver = Solver(bfsNodeBudget = 100_000, beamWidth = 200, maxBeamMoves = 30)

    @Test
    fun `already solved boards need zero moves`() {
        val result = solver.solve(Board.parse("abc"))
        assertIs<Solver.Result.Solved>(result)
        assertTrue(result.moves.isEmpty())
    }

    @Test
    fun `finds the one-move solution optimally`() {
        val result = solver.solveOptimal(Board.parse("aabab"))
        assertIs<Solver.Result.Solved>(result)
        assertEquals(1, result.moves.size)
        assertTrue(result.optimal)
    }

    @Test
    fun `solutions replay to a solved board`() {
        val start = Board.parse("aba\nbcb\naba")
        val result = solver.solve(start)
        assertIs<Solver.Result.Solved>(result)
        var board = start
        for (move in result.moves) {
            board = checkNotNull(GameEngine.apply(board, move.origin, move.target)) {
                "Solver emitted an illegal move: $move"
            }
        }
        assertTrue(GameEngine.isSolved(board))
    }

    @Test
    fun `expand never returns no-op moves`() {
        val board = Board.parse("albma")
        for ((move, next) in solver.expand(board)) {
            assertTrue(next != board, "Move $move did not change the board")
        }
    }

    @Test
    fun `a board with no action letters is proven unsolvable`() {
        // Only 'a's and a 'd': nothing can ever move.
        val result = solver.solveOptimal(Board.parse("ada"))
        assertIs<Solver.Result.ProvenUnsolvable>(result)
    }

    @Test
    fun `hint returns a legal first move`() {
        val board = Board.parse("aabab")
        val hint = assertNotNull(solver.hint(board))
        assertNotNull(GameEngine.apply(board, hint.origin, hint.target))
    }

    @Test
    fun `disorder is zero exactly for solved boards`() {
        assertEquals(0, Solver.disorder(Board.parse("abc\nbcd")))
        assertTrue(Solver.disorder(Board.parse("bab")) > 0)
    }

    @Test
    fun `best-first search solves what bfs solves`() {
        val result = solver.solveBestFirst(Board.parse("aba\nbcb\naba"), nodeBudget = 200_000)
        assertIs<Solver.Result.Solved>(result)
        var board = Board.parse("aba\nbcb\naba")
        for (move in result.moves) {
            board = checkNotNull(GameEngine.apply(board, move.origin, move.target))
        }
        assertTrue(GameEngine.isSolved(board))
    }

    @Test
    fun `immediate letters appear in expansion with null target`() {
        val board = Board.parse("blbab")
        val lMoves = solver.expand(board).filter { it.first.origin == Pos(0, 1) }
        assertTrue(lMoves.isNotEmpty())
        assertTrue(lMoves.all { it.first.target == null })
    }
}
