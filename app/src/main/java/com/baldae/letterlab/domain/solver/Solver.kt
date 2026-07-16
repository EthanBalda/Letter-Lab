package com.baldae.letterlab.domain.solver

import com.baldae.letterlab.domain.Board
import com.baldae.letterlab.domain.GameEngine
import com.baldae.letterlab.domain.Letters
import com.baldae.letterlab.domain.Pos
import com.baldae.letterlab.domain.Selection

/** One player action: an origin letter and, unless it acts immediately, a target. */
data class Move(val origin: Pos, val target: Pos?)

/**
 * Search-based solver over the game engine. Used by tests to prove the
 * campaign solvable and calibrate par values, and by the in-game hint
 * feature. Pure Kotlin, no Android dependencies.
 */
class Solver(
    /** Max states explored by breadth-first search before giving up on optimality. */
    private val bfsNodeBudget: Int = 400_000,
    /** Candidate boards kept per generation during beam search. */
    private val beamWidth: Int = 400,
    /** Max solution length attempted by beam search. */
    private val maxBeamMoves: Int = 80,
) {

    sealed interface Result {
        /** [optimal] is true only when found by exhaustive BFS. */
        data class Solved(val moves: List<Move>, val optimal: Boolean) : Result

        /** The full reachable space was exhausted without a solution. */
        data object ProvenUnsolvable : Result

        /** Budgets ran out before an answer either way. */
        data object Unknown : Result
    }

    /** Every legal, non-no-op move from [board], paired with its outcome. */
    fun expand(board: Board): List<Pair<Move, Board>> {
        val results = mutableListOf<Pair<Move, Board>>()
        for (pos in board.positions()) {
            when (val selection = GameEngine.selectionFor(board, pos)) {
                is Selection.Immediate -> {
                    val next = GameEngine.apply(board, pos, null)
                    if (next != null && next != board) results += Move(pos, null) to next
                }
                is Selection.NeedsTarget -> {
                    for (target in selection.targets) {
                        val next = GameEngine.apply(board, pos, target)
                        if (next != null && next != board) results += Move(pos, target) to next
                    }
                }
                Selection.NotSelectable -> Unit
            }
        }
        return results
    }

    /**
     * BFS first (optimal within budget), falling back to beam search.
     * Boards beyond [BFS_CELL_LIMIT] cells go straight to beam search:
     * their branching factor makes exhaustive search hopeless anyway.
     */
    fun solve(start: Board): Result {
        if (start.rows * start.cols <= BFS_CELL_LIMIT) {
            val bfs = solveOptimal(start)
            if (bfs !is Result.Unknown) return bfs
        }
        return solveAny(start)
    }

    /** Exhaustive breadth-first search; optimal when it succeeds. */
    fun solveOptimal(start: Board): Result {
        if (GameEngine.isSolved(start)) return Result.Solved(emptyList(), optimal = true)
        val startKey = start.serialize()
        // parent[key] = (parentKey, move that produced key). The queue holds
        // serialized keys, not Board objects, to keep the search memory-lean.
        val parent = HashMap<String, Pair<String, Move>?>()
        parent[startKey] = null
        val queue = ArrayDeque<String>()
        queue += startKey
        var explored = 0

        while (queue.isNotEmpty()) {
            val boardKey = queue.removeFirst()
            if (++explored > bfsNodeBudget) return Result.Unknown
            val board = Board.deserialize(boardKey)
            for ((move, next) in expand(board)) {
                val key = next.serialize()
                if (key in parent) continue
                parent[key] = boardKey to move
                if (GameEngine.isSolved(next)) {
                    return Result.Solved(reconstruct(parent, key), optimal = true)
                }
                queue += key
            }
        }
        // Queue drained within budget: the whole reachable space had no solution.
        return Result.ProvenUnsolvable
    }

    private fun reconstruct(
        parent: Map<String, Pair<String, Move>?>,
        goalKey: String,
    ): List<Move> {
        val moves = mutableListOf<Move>()
        var key = goalKey
        while (true) {
            val (parentKey, move) = parent[key] ?: break
            moves += move
            key = parentKey
        }
        return moves.reversed()
    }

    /**
     * Greedy beam search guided by [disorder]. Fast on large boards; the
     * solution it finds is an upper bound, not necessarily optimal.
     */
    fun solveAny(start: Board): Result {
        if (GameEngine.isSolved(start)) return Result.Solved(emptyList(), optimal = false)
        var frontier = listOf(start to emptyList<Move>())
        val visited = HashSet<String>()
        visited += start.serialize()

        repeat(maxBeamMoves) {
            val candidates = mutableListOf<Triple<Int, Board, List<Move>>>()
            for ((board, path) in frontier) {
                for ((move, next) in expand(board)) {
                    val key = next.serialize()
                    if (!visited.add(key)) continue
                    val newPath = path + move
                    if (GameEngine.isSolved(next)) {
                        return Result.Solved(newPath, optimal = false)
                    }
                    candidates += Triple(disorder(next), next, newPath)
                }
            }
            if (candidates.isEmpty()) return Result.Unknown
            candidates.sortBy { it.first }
            frontier = candidates.take(beamWidth).map { it.second to it.third }
        }
        return Result.Unknown
    }

    /**
     * Weighted A*: orders states by moves-so-far + [weight] × [disorder].
     * Not guaranteed optimal, but finds far shorter solutions than greedy
     * beam search on large boards. Used by tooling to calibrate pars.
     */
    fun solveBestFirst(start: Board, weight: Double = 1.5, nodeBudget: Int = 1_500_000): Result {
        if (GameEngine.isSolved(start)) return Result.Solved(emptyList(), optimal = false)
        data class Node(val key: String, val g: Int, val f: Double)

        val startKey = start.serialize()
        val parent = HashMap<String, Pair<String, Move>?>()
        parent[startKey] = null
        val open = java.util.PriorityQueue<Node>(compareBy { it.f })
        open += Node(startKey, 0, weight * disorder(start))
        var explored = 0

        while (open.isNotEmpty()) {
            val node = open.poll()
            if (++explored > nodeBudget) return Result.Unknown
            val board = Board.deserialize(node.key)
            for ((move, next) in expand(board)) {
                val key = next.serialize()
                if (key in parent) continue
                parent[key] = node.key to move
                if (GameEngine.isSolved(next)) {
                    return Result.Solved(reconstruct(parent, key), optimal = false)
                }
                open += Node(key, node.g + 1, node.g + 1 + weight * disorder(next))
            }
        }
        return Result.Unknown
    }

    /** First move of the best solution found within the budgets, or null. */
    fun hint(board: Board): Move? =
        (solve(board) as? Result.Solved)?.moves?.firstOrNull()

    companion object {
        /** Boards larger than this skip BFS in [solve]. */
        const val BFS_CELL_LIMIT = 20

        /**
         * How far from solved a board is: for every row and column, the
         * smaller number of adjacent out-of-order pairs across the two
         * allowed reading directions. Zero for a solved board.
         */
        fun disorder(board: Board): Int {
            var total = 0
            for (r in 0 until board.rows) {
                total += lineDisorder(board.rowPositions(r).map { board[it].letter })
            }
            for (c in 0 until board.cols) {
                total += lineDisorder(board.colPositions(c).map { board[it].letter })
            }
            return total
        }

        private fun lineDisorder(letters: List<Char>): Int {
            val seq = letters.filter { it != Letters.EMPTY }
            val asc = seq.zipWithNext().count { (a, b) -> a > b }
            val desc = seq.zipWithNext().count { (a, b) -> a < b }
            return minOf(asc, desc)
        }
    }
}
