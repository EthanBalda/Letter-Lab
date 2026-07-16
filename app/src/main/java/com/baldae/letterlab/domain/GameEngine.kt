package com.baldae.letterlab.domain

/** What happens when the player taps a letter to pick it up. */
sealed interface Selection {
    /** The letter needs a second tap on one of [targets] to act. */
    data class NeedsTarget(val targets: Set<Pos>) : Selection

    /** The letter acts the moment it is selected (l, m). */
    data object Immediate : Selection

    data object NotSelectable : Selection
}

/**
 * One letter physically travelling during a move. [cell] is the content that
 * moved (as it was before the move). When [vanishes] is true the letter is
 * destroyed on arrival — it slid into a d, was wiped by a passing d, or ate
 * itself into nothing — and [from] == [to] means it perished in place.
 */
data class TileMove(
    val from: Pos,
    val to: Pos,
    val cell: Cell,
    val vanishes: Boolean = false,
)

/** A move's outcome: the new board plus the physical journeys that led to it. */
data class MoveResult(
    val board: Board,
    val tileMoves: List<TileMove>,
)

/**
 * Pure rules engine for LetterLab. Stateless: every function takes a [Board]
 * and returns a new one, along with a deterministic trace of which letters
 * travelled where — the UI animates the trace, the engine stays UI-free.
 *
 * Two board-wide rules apply to every mechanic that slides letters around
 * (f force, g grab, k kick, l lovely, m monstrous):
 *
 * - `d` never moves. A letter pushed into a d's cell is destroyed, and the
 *   cell that would receive the d's content receives a blank instead.
 *   Exception: when a d is itself grabbed or kicked, it does move — and
 *   deletes everything between its start and destination.
 * - `i` never moves and is transparent: sliding letters pass over it.
 */
object GameEngine {

    fun isSelectable(board: Board, pos: Pos): Boolean =
        board[pos].letter in Letters.SELECTABLE

    fun selectionFor(board: Board, pos: Pos): Selection {
        val letter = board[pos].letter
        if (letter !in Letters.SELECTABLE) return Selection.NotSelectable
        if (letter in Letters.IMMEDIATE) return Selection.Immediate
        val targets = targetsFor(board, pos)
        return Selection.NeedsTarget(targets)
    }

    /** Legal second-tap targets for the letter at [origin]. */
    fun targetsFor(board: Board, origin: Pos): Set<Pos> {
        val letter = board[origin].letter
        val swapRange = Letters.swapRange(letter)
        return when {
            // b/c/j: the four cells exactly `range` steps away, cardinally.
            swapRange != null ->
                Dir.entries.mapNotNull { dir ->
                    origin.step(dir, swapRange).takeIf { it in board && board[it].letter != 'i' }
                }.toSet()

            // e eats any adjacent cell (including blanks — at its own peril).
            letter == 'e' ->
                neighbors(board, origin).filter { board[it].letter != 'i' }.toSet()

            // f only uses the tap to choose a direction; any neighbour works.
            letter == 'f' -> neighbors(board, origin).toSet()

            // k boots an adjacent, non-empty, non-immovable letter.
            letter == 'k' ->
                neighbors(board, origin)
                    .filter { board[it].letter != Letters.EMPTY && board[it].letter != 'i' }
                    .toSet()

            // g reaches along its whole row and column.
            letter == 'g' ->
                (board.rowPositions(origin.row) + board.colPositions(origin.col))
                    .filter {
                        it != origin &&
                            board[it].letter != Letters.EMPTY &&
                            board[it].letter != 'i'
                    }.toSet()

            // h has unlimited range. It cannot hold another h, and it needs
            // something in hand before it can target a blank cell.
            letter == 'h' ->
                board.positions().filter { pos ->
                    pos != origin &&
                        board[pos].letter != 'i' &&
                        board[pos].letter != 'h' &&
                        (board[origin].held != null || board[pos].letter != Letters.EMPTY)
                }.toSet()

            else -> emptySet()
        }
    }

    /**
     * Applies the move for the letter at [origin]. [target] must be null for
     * immediate letters (l, m) and a member of [targetsFor] otherwise.
     * Returns null for an illegal move.
     */
    fun apply(board: Board, origin: Pos, target: Pos?): Board? =
        applyWithTrace(board, origin, target)?.board

    /** Like [apply], but also reports every letter's journey for animation. */
    fun applyWithTrace(board: Board, origin: Pos, target: Pos?): MoveResult? {
        val letter = board[origin].letter
        if (letter !in Letters.SELECTABLE) return null

        if (letter in Letters.IMMEDIATE) {
            return when (letter) {
                'l' -> applyLovely(board, origin)
                'm' -> applyMonstrous(board, origin)
                else -> null
            }
        }

        if (target == null || target !in targetsFor(board, origin)) return null

        return when (letter) {
            'b', 'c', 'j' -> applySwap(board, origin, target)
            'e' -> applyEat(board, origin, target)
            'f' -> applyForce(board, dirBetween(origin, target)!!)
            'g' -> applyGrab(board, origin, target)
            'k' -> applyKick(board, origin, dirBetween(origin, target)!!)
            'h' -> applyHold(board, origin, target)
            else -> null
        }
    }

    /**
     * Win condition: every row and every column must read in alphabetical
     * order — ascending or descending — ignoring blank cells. An h counts
     * as 'h' no matter what it holds.
     */
    fun isSolved(board: Board): Boolean {
        for (r in 0 until board.rows) {
            if (!isMonotone(board.rowPositions(r).map { board[it].letter })) return false
        }
        for (c in 0 until board.cols) {
            if (!isMonotone(board.colPositions(c).map { board[it].letter })) return false
        }
        return true
    }

    fun dirBetween(origin: Pos, target: Pos): Dir? = when {
        target.row == origin.row && target.col > origin.col -> Dir.RIGHT
        target.row == origin.row && target.col < origin.col -> Dir.LEFT
        target.col == origin.col && target.row > origin.row -> Dir.DOWN
        target.col == origin.col && target.row < origin.row -> Dir.UP
        else -> null
    }

    // ---------------------------------------------------------------- swaps

    private fun applySwap(board: Board, origin: Pos, target: Pos): MoveResult {
        val originCell = board[origin]
        val targetCell = board[target]
        // Touching a d deletes the letter that dared to interact with it.
        if (targetCell.letter == 'd') {
            return MoveResult(
                board.with(origin to Cell.EMPTY),
                listOf(TileMove(origin, target, originCell, vanishes = true)),
            )
        }
        val moves = buildList {
            add(TileMove(origin, target, originCell))
            if (!targetCell.isEmpty) add(TileMove(target, origin, targetCell))
        }
        return MoveResult(board.with(origin to targetCell, target to originCell), moves)
    }

    // ------------------------------------------------------------------ eat

    private fun applyEat(board: Board, origin: Pos, target: Pos): MoveResult {
        // e becomes whatever it eats — a blank destroys it, and it is the one
        // letter that can swallow a d whole.
        val targetCell = board[target]
        val moves =
            if (targetCell.isEmpty) {
                // Ate nothing; the e perishes in place.
                listOf(TileMove(origin, origin, board[origin], vanishes = true))
            } else {
                listOf(TileMove(target, origin, targetCell))
            }
        return MoveResult(board.with(origin to targetCell, target to Cell.EMPTY), moves)
    }

    // ---------------------------------------------------------------- force

    private fun applyForce(board: Board, dir: Dir): MoveResult {
        var result = MoveResult(board, emptyList())
        val lines: List<List<Pos>> = when (dir) {
            Dir.LEFT, Dir.RIGHT -> (0 until board.rows).map { board.rowPositions(it) }
            Dir.UP, Dir.DOWN -> (0 until board.cols).map { board.colPositions(it) }
        }
        for (line in lines) {
            // Order the line so that each position's donor (the cell whose
            // letter slides into it) is the next entry, wrapping circularly.
            val shiftOrder = when (dir) {
                Dir.RIGHT, Dir.DOWN -> line.reversed()
                Dir.LEFT, Dir.UP -> line
            }
            val step = cycle(result.board, shiftOrder)
            result = MoveResult(step.board, result.tileMoves + step.tileMoves)
        }
        return result
    }

    // ----------------------------------------------------------------- grab

    private fun applyGrab(board: Board, origin: Pos, target: Pos): MoveResult {
        val dir = dirBetween(origin, target) ?: return MoveResult(board, emptyList())
        val path = pathBetween(origin, target, dir)
        if (board[target].letter == 'd') {
            // Grabbing a d drags it next to g and wipes everything in between.
            return slingDelete(
                board, path,
                payload = target,
                destination = path.firstOrNull { board[it].letter != 'i' },
            )
        }
        return cycle(board, path.reversed())
    }

    // ----------------------------------------------------------------- kick

    private fun applyKick(board: Board, origin: Pos, dir: Dir): MoveResult {
        val path = rayToEdge(board, origin, dir)
        if (path.isEmpty()) return MoveResult(board, emptyList())
        if (board[path.first()].letter == 'd') {
            // Kicking a d boots it to the far edge and wipes everything in between.
            return slingDelete(
                board, path,
                payload = path.first(),
                destination = path.lastOrNull { board[it].letter != 'i' },
            )
        }
        return cycle(board, path)
    }

    // --------------------------------------------------------------- lovely

    /** l kicks its neighbour in every direction, blanks included. */
    private fun applyLovely(board: Board, origin: Pos): MoveResult {
        var result = MoveResult(board, emptyList())
        for (dir in Dir.entries) {
            val step = applyKick(result.board, origin, dir)
            result = MoveResult(step.board, result.tileMoves + step.tileMoves)
        }
        return result
    }

    // ------------------------------------------------------------ monstrous

    /** m grabs the edge cell of every direction inward, blanks included. */
    private fun applyMonstrous(board: Board, origin: Pos): MoveResult {
        var result = MoveResult(board, emptyList())
        for (dir in Dir.entries) {
            val edge = edgePos(board, origin, dir)
            if (edge == origin) continue
            val path = pathBetween(origin, edge, dir)
            val step = if (result.board[edge].letter == 'd') {
                slingDelete(
                    result.board, path,
                    payload = edge,
                    destination = path.firstOrNull { result.board[it].letter != 'i' },
                )
            } else {
                cycle(result.board, path.reversed())
            }
            result = MoveResult(step.board, result.tileMoves + step.tileMoves)
        }
        return result
    }

    // ----------------------------------------------------------------- hold

    private fun applyHold(board: Board, origin: Pos, target: Pos): MoveResult {
        val h = board[origin]
        val targetCell = board[target]
        // Grabbing a d destroys the h and whatever it held; the d relocates.
        if (targetCell.letter == 'd') {
            return MoveResult(
                board.with(origin to Cell('d'), target to Cell.EMPTY),
                listOf(
                    TileMove(origin, origin, h, vanishes = true),
                    TileMove(target, origin, targetCell),
                ),
            )
        }
        val newHeld = targetCell.letter.takeIf { it != Letters.EMPTY }
        val dropped = h.held?.let { Cell(it) } ?: Cell.EMPTY
        val moves = buildList {
            if (!targetCell.isEmpty) add(TileMove(target, origin, targetCell))
            if (!dropped.isEmpty) add(TileMove(origin, target, dropped))
        }
        return MoveResult(
            board.with(origin to Cell('h', newHeld), target to dropped),
            moves,
        )
    }

    // ------------------------------------------------------------ primitives

    /**
     * Rotates cells along [order]: each position receives the cell of the
     * next position in the list, and the last receives the first's (circular).
     * `i` cells are removed from the rotation (transparent); `d` cells stay
     * put, destroy their incoming letter, and donate a blank.
     */
    private fun cycle(board: Board, order: List<Pos>): MoveResult {
        val ring = order.filter { board[it].letter != 'i' }
        if (ring.size < 2) return MoveResult(board, emptyList())
        val changes = HashMap<Pos, Cell>(ring.size)
        val moves = mutableListOf<TileMove>()
        for (idx in ring.indices) {
            val pos = ring[idx]
            if (board[pos].letter == 'd') {
                // The d keeps its cell; the letter flowing toward it perishes.
                val victimPos = ring[(idx + 1) % ring.size]
                val victim = board[victimPos]
                if (!victim.isEmpty && victim.letter != 'd') {
                    moves += TileMove(victimPos, pos, victim, vanishes = true)
                }
                continue
            }
            val donorPos = ring[(idx + 1) % ring.size]
            val donor = board[donorPos]
            if (donor.letter == 'd') {
                changes[pos] = Cell.EMPTY
            } else {
                changes[pos] = donor
                if (!donor.isEmpty) moves += TileMove(donorPos, pos, donor)
            }
        }
        return MoveResult(board.with(changes), moves)
    }

    /**
     * Moves a grabbed/kicked `d` from [payload] to [destination] and blanks
     * every other non-immovable cell on [path] (the d's trail of destruction).
     */
    private fun slingDelete(
        board: Board,
        path: List<Pos>,
        payload: Pos,
        destination: Pos?,
    ): MoveResult {
        val changes = HashMap<Pos, Cell>()
        val moves = mutableListOf<TileMove>()
        for (pos in path) {
            val cell = board[pos]
            if (cell.letter == 'i') continue
            changes[pos] = Cell.EMPTY
            if (pos != payload && !cell.isEmpty) {
                moves += TileMove(pos, pos, cell, vanishes = true)
            }
        }
        if (destination != null) {
            changes[destination] = Cell('d')
            if (destination != payload) {
                moves += TileMove(payload, destination, board[payload])
            }
        }
        return MoveResult(board.with(changes), moves)
    }

    /** Positions from just past [origin] up to and including [target]. */
    private fun pathBetween(origin: Pos, target: Pos, dir: Dir): List<Pos> {
        val path = mutableListOf<Pos>()
        var pos = origin + dir
        while (true) {
            path += pos
            if (pos == target) break
            pos += dir
        }
        return path
    }

    /** Positions from just past [origin] to the board edge, in [dir] order. */
    private fun rayToEdge(board: Board, origin: Pos, dir: Dir): List<Pos> {
        val path = mutableListOf<Pos>()
        var pos = origin + dir
        while (pos in board) {
            path += pos
            pos += dir
        }
        return path
    }

    private fun edgePos(board: Board, origin: Pos, dir: Dir): Pos = when (dir) {
        Dir.UP -> Pos(0, origin.col)
        Dir.DOWN -> Pos(board.rows - 1, origin.col)
        Dir.LEFT -> Pos(origin.row, 0)
        Dir.RIGHT -> Pos(origin.row, board.cols - 1)
    }

    private fun neighbors(board: Board, origin: Pos): List<Pos> =
        Dir.entries.map { origin + it }.filter { it in board }

    private fun isMonotone(letters: List<Char>): Boolean {
        val seq = letters.filter { it != Letters.EMPTY }
        return seq.zipWithNext().all { (a, b) -> a <= b } ||
            seq.zipWithNext().all { (a, b) -> a >= b }
    }
}
