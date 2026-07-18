package com.baldae.letterlab.domain.solver

import com.baldae.letterlab.data.LevelRepository
import com.baldae.letterlab.domain.GameEngine
import java.io.File
import org.junit.Test

/**
 * Tooling, not a gate: solves every campaign level with generous budgets and
 * writes build/calibration.txt with best-known solution lengths, whether the
 * level's tutorial letter is actually required by the found solution, and a
 * par verdict. Used to keep pars honest (par >= best-known keeps three stars
 * reachable). Hard solvability assertions live in CampaignSolvabilityTest.
 */
class CampaignCalibrationTest {

    @Test
    fun `write calibration report`() {
        val catalog = LevelRepository.parse(File("src/main/assets/levels.json").readText())
        val report = StringBuilder()
        report.appendLine("id | name | cells | par | best | optimal | tutorialUsed | parVerdict | ms")

        for (level in catalog.allLevels) {
            val board = level.board()
            val cells = board.rows * board.cols
            val solver = Solver(bfsNodeBudget = 300_000, beamWidth = 600, maxBeamMoves = 90)
            val start = System.currentTimeMillis()

            var result: Solver.Result = if (cells <= 16) solver.solveOptimal(board)
            else Solver.Result.Unknown
            if (result is Solver.Result.Unknown) {
                for (weight in listOf(1.5, 2.5, 4.0)) {
                    result = solver.solveBestFirst(board, weight, nodeBudget = 250_000)
                    if (result !is Solver.Result.Unknown) break
                }
            }
            if (result is Solver.Result.Unknown) result = solver.solveAny(board)
            val ms = System.currentTimeMillis() - start

            val line = when (result) {
                is Solver.Result.Solved -> {
                    // Which letters actually act in the found solution?
                    val used = mutableSetOf<Char>()
                    var replay = board
                    for (move in result.moves) {
                        used += GameEngine.effective(replay, move.origin)
                        replay = GameEngine.apply(replay, move.origin, move.target) ?: break
                    }
                    // Passive letters (d, i, n) never act; their tutorials
                    // demonstrate by shaping the level instead.
                    val tutorial = level.tutorial?.letter?.singleOrNull()
                        ?.takeIf { it !in "din" }
                    val tutorialUsed = tutorial?.let { if (it in used) "yes" else "NO" } ?: "-"
                    val best = result.moves.size
                    val verdict = when {
                        level.par < best -> "PAR_TOO_LOW"
                        level.par > best + 2 -> "PAR_TOO_HIGH"
                        else -> "ok"
                    }
                    "${level.id} | ${level.name} | ${board.rows}x${board.cols} | ${level.par} | " +
                        "$best | ${result.optimal} | $tutorialUsed | $verdict | $ms"
                }
                Solver.Result.ProvenUnsolvable ->
                    "${level.id} | ${level.name} | ${board.rows}x${board.cols} | ${level.par} | " +
                        "UNSOLVABLE | - | - | BROKEN | $ms"
                Solver.Result.Unknown ->
                    "${level.id} | ${level.name} | ${board.rows}x${board.cols} | ${level.par} | " +
                        "UNKNOWN | - | - | CHECK | $ms"
            }
            report.appendLine(line)
            println(line)
        }

        File("build/calibration.txt").apply {
            parentFile.mkdirs()
            writeText(report.toString())
        }
    }
}
