package com.baldae.letterlab.domain.solver

import com.baldae.letterlab.data.LevelRepository
import java.io.File
import org.junit.Test

/** One-off tooling: tighten best-known solution lengths for the hard levels. */
class TargetedAnalysisTest {

    @org.junit.Ignore("Tooling: takes minutes. Remove @Ignore to re-run the analysis.")
    @Test
    fun `tighten bounds on hard levels`() {
        val catalog = LevelRepository.parse(File("src/main/assets/levels.json").readText())
        val targets = listOf(6, 9, 13, 15, 19, 21, 22, 24)
        val solver = Solver()
        val out = StringBuilder("level | weight | moves | ms\n")
        for (id in targets) {
            val board = catalog.level(id)!!.board()
            var best = Int.MAX_VALUE
            for (weight in listOf(1.5, 2.0, 3.0)) {
                val start = System.currentTimeMillis()
                val result = solver.solveBestFirst(board, weight = weight, nodeBudget = 400_000)
                val ms = System.currentTimeMillis() - start
                val moves = (result as? Solver.Result.Solved)?.moves?.size ?: -1
                if (moves in 1 until best) best = moves
                val line = "$id | $weight | $moves | $ms"
                out.appendLine(line)
                println(line)
            }
            out.appendLine("$id => best $best")
        }
        File("build/targeted-report.txt").writeText(out.toString())
    }
}
