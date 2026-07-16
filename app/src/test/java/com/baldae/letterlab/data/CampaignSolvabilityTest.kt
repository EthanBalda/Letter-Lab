package com.baldae.letterlab.data

import com.baldae.letterlab.domain.GameEngine
import com.baldae.letterlab.domain.solver.Solver
import java.io.File
import kotlin.test.assertTrue
import org.junit.Test

/**
 * The release gate for level content: every shipped level must be solvable
 * by the solver, and the solution must replay cleanly through the engine.
 * Deterministic: the solver has no randomness, so a level that passes once
 * always passes.
 */
class CampaignSolvabilityTest {

    @Test
    fun `every campaign level is solvable and its solution replays`() {
        val catalog = LevelRepository.parse(File("src/main/assets/levels.json").readText())
        val failures = mutableListOf<String>()

        for (level in catalog.allLevels) {
            val board = level.board()
            val solver = Solver(bfsNodeBudget = 200_000, beamWidth = 600, maxBeamMoves = 80)
            // Beam first (fast); exhaustive BFS as the fallback for any level
            // greedy search can't crack.
            var result = solver.solveAny(board)
            if (result !is Solver.Result.Solved) result = solver.solve(board)

            val solved = result as? Solver.Result.Solved
            if (solved == null) {
                failures += "Level ${level.id} '${level.name}': no solution found"
                continue
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
