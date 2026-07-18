package com.baldae.letterlab.data

import com.baldae.letterlab.domain.GameEngine
import com.baldae.letterlab.domain.solver.Solver
import java.io.File
import kotlin.test.assertTrue
import org.junit.Test

/**
 * The release gate for level content: every shipped level must have a
 * solver-found solution that replays cleanly through the engine AND fits
 * within par — otherwise a three-star clear (and the Perfectionist
 * achievement) would be impossible. Deterministic: the solver has no
 * randomness, so a level that passes once always passes.
 */
class CampaignSolvabilityTest {

    @Test
    fun `every campaign level is three-star solvable and its solution replays`() {
        val catalog = LevelRepository.parse(File("src/main/assets/levels.json").readText())
        val failures = mutableListOf<String>()

        for (level in catalog.allLevels) {
            val board = level.board()
            val solver = Solver(bfsNodeBudget = 300_000, beamWidth = 600, maxBeamMoves = 90)

            // Cheapest-first ladder; stop as soon as a within-par solution shows up.
            var best: Solver.Result.Solved? = null
            val attempts = sequence {
                yield(solver.solveAny(board))
                for (w in listOf(1.5, 2.5, 4.0)) {
                    yield(solver.solveBestFirst(board, w, nodeBudget = 250_000))
                }
                if (board.rows * board.cols <= 16) yield(solver.solveOptimal(board))
            }
            for (result in attempts) {
                val solved = result as? Solver.Result.Solved ?: continue
                if (best == null || solved.moves.size < best!!.moves.size) best = solved
                if (best!!.moves.size <= level.par) break
            }

            val solved = best
            if (solved == null) {
                failures += "Level ${level.id} '${level.name}': no solution found"
                continue
            }
            if (solved.moves.size > level.par) {
                failures += "Level ${level.id} '${level.name}': best found " +
                    "${solved.moves.size} moves exceeds par ${level.par} — 3 stars unreachable"
            }
            var replay = board
            for (move in solved.moves) {
                val next = GameEngine.apply(replay, move.origin, move.target)
                if (next == null) {
                    failures += "Level ${level.id}: solver move $move is illegal"
                    break
                }
                replay = next
            }
            if (!GameEngine.isSolved(replay)) {
                failures += "Level ${level.id}: replayed solution does not solve the board"
            }
        }

        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
