package com.baldae.letterlab.domain.solver

import com.baldae.letterlab.data.LevelRepository
import java.io.File
import org.junit.Test

/**
 * Tooling, not a gate: solves the whole campaign and writes
 * build/campaign-report.txt with solvability, solution lengths and timing.
 * Used to calibrate par values. Never fails the build by itself —
 * hard solvability assertions live in CampaignSolvabilityTest.
 */
class CampaignReportTest {

    @org.junit.Ignore("Tooling: takes minutes. Remove @Ignore to regenerate the report.")
    @Test
    fun `write campaign analysis report`() {
        val catalog = LevelRepository.parse(File("src/main/assets/levels.json").readText())
        val report = StringBuilder()
        report.appendLine("level | name | cells | par | result | moves | optimal | ms")

        for (level in catalog.allLevels) {
            val board = level.board()
            val solver = Solver(bfsNodeBudget = 1_500_000, beamWidth = 600, maxBeamMoves = 80)
            val startMs = System.currentTimeMillis()
            // Exhaustive BFS on small boards; weighted A* (near-optimal) on the rest.
            var result: Solver.Result =
                if (board.rows * board.cols <= Solver.BFS_CELL_LIMIT) {
                    solver.solveOptimal(board)
                } else Solver.Result.Unknown
            if (result is Solver.Result.Unknown) {
                result = solver.solveBestFirst(board, weight = 1.5, nodeBudget = 1_500_000)
            }
            if (result is Solver.Result.Unknown) {
                result = solver.solveAny(board)
            }
            val elapsed = System.currentTimeMillis() - startMs
            val line = when (result) {
                is Solver.Result.Solved ->
                    "${level.id} | ${level.name} | ${board.rows}x${board.cols} | ${level.par} | " +
                        "SOLVED | ${result.moves.size} | ${result.optimal} | $elapsed"
                Solver.Result.ProvenUnsolvable ->
                    "${level.id} | ${level.name} | ${board.rows}x${board.cols} | ${level.par} | " +
                        "UNSOLVABLE | - | - | $elapsed"
                Solver.Result.Unknown ->
                    "${level.id} | ${level.name} | ${board.rows}x${board.cols} | ${level.par} | " +
                        "UNKNOWN | - | - | $elapsed"
            }
            report.appendLine(line)
            println(line)
        }

        File("build/campaign-report.txt").apply {
            parentFile.mkdirs()
            writeText(report.toString())
        }
    }
}
