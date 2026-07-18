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
 * (f force, g grab, k kick, l lovely, m monstrous, y yeet):
 *
 * - `d` never moves. A letter pushed into a d's cell is destroyed, and the
 *   cell that would receive the d's content receives a blank instead.
 *   Exception: when a d is itself grabbed or kicked, it does move — and
 *   deletes everything between its start and destination.
 * - `i` never moves and is transparent: sliding letters pass over it.
 *
 * Both rules — and every letter's ability — are read through [effective]:
 * a letter cardinally adjacent to an active `n` behaves exactly like an
 * 'a' (cannot act, cannot delete, cannot protect itself).
 */
object GameEngine {

    /** True when the n at [pos] radiates: no other n sits cardinally adjacent. */
    private fun nActive(board: Board, pos: Pos): Boolean =
        Dir.entries.none { dir ->
            val q = pos + dir
            q in board && board[q].letter == 'n'
        }

    /** True when the letter at [pos] sits inside an active n's aura. */
    fun isNullified(board: Board, pos: Pos): Boolean =
        board[pos].letter != Letters.EMPTY &&
            Dir.entries.any { dir ->
                val q = pos + dir
                q in board && board[q].letter == 'n' && nActive(board, q)
            }

    /**
     * The rule identity of the cell at [pos]: its real letter, or 'a' when
     * an adjacent active n has nullified it. Sorting always uses the real
     * letter; behaviour always uses this one.
     */
    fun effective(board: Board, pos: Pos): Char =
        if (isNullified(board, pos)) 'a' else board[pos].letter

    fun isSelectable(board: Board, pos: Pos): Boolean =
        effective(board, pos) in Letters.SELECTABLE

    fun selectionFor(board: Board, pos: Pos): Selection {
        val letter = effective(board, pos)
        if (letter !in Letters.SELECTABLE) return Selection.NotSelectable
        if (letter in Letters.IMMEDIATE) return Selection.Immediate
        val targets = targetsFor(board, pos)
        return Selection.NeedsTarget(targets)
    }

    /** Legal second-tap targets for the letter at [origin]. */
    fun targetsFor(board: Board, origin: Pos): Set<Pos> {
        val letter = effective(board, origin)
        val swapRange = Letters.swapRange(letter)

        /** Nothing may target an i unless an n has nullified it. */
        fun touchable(pos: Pos): Boolean = effective(board, pos) != 'i'

        return when {
            // b/c/j: the four cells exactly `range` steps away, cardinally.
            swapRange != null ->
                Dir.entries.mapNotNull { dir ->
                    origin.step(dir, swapRange).takeIf { it in board && touchable(it) }
                }.toSet()

            // e eats any adjacent cell (including blanks — at its own peril).
            letter == 'e' ->
                neighbors(board, origin).filter { touchable(it) }.toSet()

            // f only uses the tap to choose a direction; any neighbour works.
            letter == 'f' -> neighbors(board, origin).toSet()

            // k boots an adjacent, non-empty, non-immovable letter.
            letter == 'k' ->
                neighbors(board, origin)
                    .filter { board[it].letter != Letters.EMPTY && touchable(it) }
                    .toSet()

            // g reaches along its whole row and column.
            letter == 'g' ->
                (board.rowPositions(origin.row) + board.colPositions(origin.col))
                    .filter {
                        it != origin &&
                            board[it].letter != Letters.EMPTY &&
                            touchable(it)
                    }.toSet()

            // h has unlimited range. It cannot hold another h, and it needs
            // something in hand before it can target a blank cell.
            letter == 'h' ->
                board.positions().filter { pos ->
                    pos != origin &&
                        touchable(pos) &&
                        board[pos].letter != 'h' &&
                        (board[origin].held != null || board[pos].letter != Letters.EMPTY)
                }.toSet()

            // o reaches any vowel on the board, no matter the distance.
            letter == 'o' ->
                board.positions().filter {
                    it != origin && board[it].letter in Letters.VOWELS && touchable(it)
                }.toSet()

            // q sweeps her row, column, and both diagonals, any distance.
            letter == 'q' ->
                board.positions().filter { pos ->
                    val dr = pos.row - origin.row
                    val dc = pos.col - origin.col
                    pos != origin &&
                        (dr == 0 || dc == 0 || dr == dc || dr == -dc) &&
                        touchable(pos)
                }.toSet()

            // r removes an adjacent, non-empty letter — but never an i.
            letter == 'r' ->
                neighbors(board, origin)
                    .filter {
                        board[it].letter != Letters.EMPTY &&
                            board[it].letter != 'i' &&
                            touchable(it)
                    }.toSet()

            // s swaps the target with the cell mirrored through itself;
            // both ends must exist and be touchable, and at least one
            // must hold something.
            letter == 's' ->
                Dir.entries.mapNotNull { dir ->
                    val target = origin + dir
                    val opposite = origin.step(dir, -1)
                    target.takeIf {
                        it in board && opposite in board &&
                            touchable(it) && touchable(opposite) &&
                            !(board[it].isEmpty && board[opposite].isEmpty) &&
                            // A d only ever destroys the letter across from it;
                            // d-with-d and d-with-blank would both be no-ops.
                            !(effective(board, it) == 'd' &&
                                (effective(board, opposite) == 'd' || board[opposite].isEmpty)) &&
                            !(effective(board, opposite) == 'd' && board[it].isEmpty)
                    }
                }.toSet()

            // t and u transform an adjacent, non-empty letter.
            letter == 't' || letter == 'u' ->
                neighbors(board, origin)
                    .filter { board[it].letter != Letters.EMPTY && touchable(it) }
                    .toSet()

            // v swaps like b (and infects what it swaps with).
            letter == 'v' ->
                neighbors(board, origin).filter { touchable(it) }.toSet()

            // w glides to any diagonal neighbour.
            letter == 'w' ->
                DIAGONALS.mapNotNull { (dr, dc) ->
                    Pos(origin.row + dr, origin.col + dc)
                        .takeIf { it in board && touchable(it) }
                }.toSet()

            // x photocopies an adjacent, non-empty letter. Copying an i is
            // impossible and copying an x is pointless.
            letter == 'x' ->
                neighbors(board, origin)
                    .filter {
                        board[it].letter != Letters.EMPTY &&
                            board[it].letter != 'i' &&
                            board[it].letter != 'x' &&
                            touchable(it)
                    }.toSet()

            // y taps a neighbour to pick its launch direction, like f.
            letter == 'y' -> neighbors(board, origin).toSet()

            // z reaches every cell on the board.
            letter == 'z' ->
                board.positions().filter { it != origin && touchable(it) }.toSet()

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
        val letter = effective(board, origin)
        if (letter !in Letters.SELECTABLE) return null

        if (letter in Letters.IMMEDIATE) {
            return when (letter) {
                'l' -> applyLovely(board, origin)
                'm' -> applyMonstrous(board, origin)
                'p' -> applyPurge(board, origin)
                else -> null
            }
        }

        if (target == null || target !in targetsFor(board, origin)) return null

        return when (letter) {
            'b', 'c', 'j', 'o', 'q', 'w', 'z' -> applySwap(board, origin, target)
            'e' -> applyEat(board, origin, target)
            'f' -> applyForce(board, dirBetween(origin, target)!!)
            'g' -> applyGrab(board, origin, target)
            'k' -> applyKick(board, origin, dirBetween(origin, target)!!)
            'h' -> applyHold(board, origin, target)
            'r' -> applyRemove(board, origin, target)
            's' -> applySpin(board, origin, target)
            't' -> applyTransform(board, origin, target, Letters::previous)
            'u' -> applyTransform(board, origin, target, Letters::next)
            'v' -> applyVirus(board, origin, target)
            'x' -> applyXerox(board, origin, target)
            'y' -> applyYeet(board, origin, dirBetween(origin, target)!!)
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
        if (effective(board, target) == 'd') {
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
        if (effective(board, target) == 'd') {
            // Grabbing a d drags it next to g and wipes everything in between.
            return slingDelete(
                board, path,
                payload = target,
                destination = path.firstOrNull { effective(board, it) != 'i' },
            )
        }
        return cycle(board, path.reversed())
    }

    // ----------------------------------------------------------------- kick

    private fun applyKick(board: Board, origin: Pos, dir: Dir): MoveResult {
        val path = rayToEdge(board, origin, dir)
        if (path.isEmpty()) return MoveResult(board, emptyList())
        if (effective(board, path.first()) == 'd') {
            // Kicking a d boots it to the far edge and wipes everything in between.
            return slingDelete(
                board, path,
                payload = path.first(),
                destination = path.lastOrNull { effective(board, it) != 'i' },
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
            val step = if (effective(result.board, edge) == 'd') {
                slingDelete(
                    result.board, path,
                    payload = edge,
                    destination = path.firstOrNull { effective(result.board, it) != 'i' },
                )
            } else {
                cycle(result.board, path.reversed())
            }
            result = MoveResult(step.board, result.tileMoves + step.tileMoves)
        }
        return result
    }

    // ---------------------------------------------------------------- purge

    /** p destroys itself and its four neighbours. Only an i survives it. */
    private fun applyPurge(board: Board, origin: Pos): MoveResult {
        val doomed = (listOf(origin) + neighbors(board, origin))
            .filter { effective(board, it) != 'i' && !board[it].isEmpty }
        return MoveResult(
            board.with(doomed.associateWith { Cell.EMPTY }),
            doomed.map { TileMove(it, it, board[it], vanishes = true) },
        )
    }

    // --------------------------------------------------------------- remove

    /**
     * r removes every copy of the target's real letter, then becomes an 'a'.
     * Removing d's costs the r its own life; removing r's includes itself.
     */
    private fun applyRemove(board: Board, origin: Pos, target: Pos): MoveResult {
        val letter = board[target].letter
        val removed = board.positions().filter { board[it].letter == letter }
        val changes = HashMap<Pos, Cell>()
        removed.forEach { changes[it] = Cell.EMPTY }
        changes[origin] = if (letter == 'd' || letter == 'r') Cell.EMPTY else Cell('a')
        val moves = buildList {
            removed.forEach { add(TileMove(it, it, board[it], vanishes = true)) }
            if (letter == 'd') add(TileMove(origin, origin, board[origin], vanishes = true))
        }
        return MoveResult(board.with(changes), moves)
    }

    // ----------------------------------------------------------------- spin

    /** s swaps the target with the cell mirrored through itself. */
    private fun applySpin(board: Board, origin: Pos, target: Pos): MoveResult {
        val dir = dirBetween(origin, target) ?: return MoveResult(board, emptyList())
        val opposite = origin.step(dir, -1)
        // A d on either end never moves; the letter across from it perishes.
        if (effective(board, target) == 'd') {
            return MoveResult(
                board.with(opposite to Cell.EMPTY),
                listOf(TileMove(opposite, target, board[opposite], vanishes = true)),
            )
        }
        if (effective(board, opposite) == 'd') {
            return MoveResult(
                board.with(target to Cell.EMPTY),
                listOf(TileMove(target, opposite, board[target], vanishes = true)),
            )
        }
        val moves = buildList {
            if (!board[target].isEmpty) add(TileMove(target, opposite, board[target]))
            if (!board[opposite].isEmpty) add(TileMove(opposite, target, board[opposite]))
        }
        return MoveResult(
            board.with(target to board[opposite], opposite to board[target]),
            moves,
        )
    }

    // ------------------------------------------------------------ transform

    /** t and u rewrite an adjacent letter one step along the alphabet. */
    private fun applyTransform(
        board: Board,
        origin: Pos,
        target: Pos,
        shift: (Char) -> Char,
    ): MoveResult {
        // Reaching into a d is as fatal as touching one.
        if (effective(board, target) == 'd') {
            return MoveResult(
                board.with(origin to Cell.EMPTY),
                listOf(TileMove(origin, target, board[origin], vanishes = true)),
            )
        }
        // Transforming an h drops whatever it held; the hands are gone.
        return MoveResult(
            board.with(target to Cell(shift(board[target].letter))),
            emptyList(),
        )
    }

    // ---------------------------------------------------------------- virus

    /** v swaps like b, and whatever it swapped with becomes a v. */
    private fun applyVirus(board: Board, origin: Pos, target: Pos): MoveResult {
        val v = board[origin]
        // A d destroys the v — and catches the virus doing it.
        if (effective(board, target) == 'd') {
            return MoveResult(
                board.with(origin to Cell.EMPTY, target to Cell('v')),
                listOf(TileMove(origin, target, v, vanishes = true)),
            )
        }
        val targetCell = board[target]
        val infected = if (targetCell.isEmpty) Cell.EMPTY else Cell('v')
        val moves = buildList {
            add(TileMove(origin, target, v))
            if (!targetCell.isEmpty) add(TileMove(target, origin, targetCell))
        }
        return MoveResult(board.with(origin to infected, target to v), moves)
    }

    // ---------------------------------------------------------------- xerox

    /** x becomes an exact copy of the target cell, held letter and all. */
    private fun applyXerox(board: Board, origin: Pos, target: Pos): MoveResult =
        MoveResult(board.with(origin to board[target]), emptyList())

    // ----------------------------------------------------------------- yeet

    /** y kicks itself: flies to the far edge, passed letters shift back. */
    private fun applyYeet(board: Board, origin: Pos, dir: Dir): MoveResult =
        cycle(board, listOf(origin) + rayToEdge(board, origin, dir))

    // ----------------------------------------------------------------- hold

    private fun applyHold(board: Board, origin: Pos, target: Pos): MoveResult {
        val h = board[origin]
        val targetCell = board[target]
        // Grabbing a d destroys the h and whatever it held; the d relocates.
        if (effective(board, target) == 'd') {
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
        val ring = order.filter { effective(board, it) != 'i' }
        if (ring.size < 2) return MoveResult(board, emptyList())
        val changes = HashMap<Pos, Cell>(ring.size)
        val moves = mutableListOf<TileMove>()
        for (idx in ring.indices) {
            val pos = ring[idx]
            if (effective(board, pos) == 'd') {
                // The d keeps its cell; the letter flowing toward it perishes.
                val victimPos = ring[(idx + 1) % ring.size]
                val victim = board[victimPos]
                if (!victim.isEmpty && effective(board, victimPos) != 'd') {
                    moves += TileMove(victimPos, pos, victim, vanishes = true)
                }
                continue
            }
            val donorPos = ring[(idx + 1) % ring.size]
            val donor = board[donorPos]
            if (effective(board, donorPos) == 'd') {
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
            if (effective(board, pos) == 'i') continue
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

    /** The four diagonal offsets, for w's waltz. */
    private val DIAGONALS = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)

    private fun neighbors(board: Board, origin: Pos): List<Pos> =
        Dir.entries.map { origin + it }.filter { it in board }

    private fun isMonotone(letters: List<Char>): Boolean {
        val seq = letters.filter { it != Letters.EMPTY }
        return seq.zipWithNext().all { (a, b) -> a <= b } ||
            seq.zipWithNext().all { (a, b) -> a >= b }
    }
}
