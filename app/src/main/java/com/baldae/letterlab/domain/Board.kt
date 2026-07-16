package com.baldae.letterlab.domain

/** A cardinal direction on the board. */
enum class Dir(val dr: Int, val dc: Int) {
    UP(-1, 0), DOWN(1, 0), LEFT(0, -1), RIGHT(0, 1)
}

/** A cell coordinate. Row 0 is the top row. */
data class Pos(val row: Int, val col: Int) {
    operator fun plus(dir: Dir): Pos = Pos(row + dir.dr, col + dir.dc)
    fun step(dir: Dir, count: Int): Pos = Pos(row + dir.dr * count, col + dir.dc * count)
}

/**
 * A single board cell. [held] is only ever non-null for an `h` that is
 * holding a letter above its head.
 */
data class Cell(val letter: Char, val held: Char? = null) {
    val isEmpty: Boolean get() = letter == Letters.EMPTY

    /** Compact token used by the save system: "b", "_", or "hb" for an h holding b. */
    fun token(): String = if (held != null) "$letter$held" else letter.toString()

    companion object {
        val EMPTY = Cell(Letters.EMPTY)

        fun fromToken(token: String): Cell {
            require(token.length in 1..2) { "Bad cell token: $token" }
            return if (token.length == 2) Cell(token[0], token[1]) else Cell(token[0])
        }
    }
}

/**
 * An immutable rectangular grid of [Cell]s.
 */
class Board(val rows: Int, val cols: Int, cells: List<Cell>) {
    private val cells: List<Cell> = cells.toList()

    init {
        require(rows > 0 && cols > 0) { "Board must have positive dimensions" }
        require(cells.size == rows * cols) {
            "Expected ${rows * cols} cells, got ${cells.size}"
        }
    }

    operator fun get(pos: Pos): Cell = cells[pos.row * cols + pos.col]

    operator fun contains(pos: Pos): Boolean =
        pos.row in 0 until rows && pos.col in 0 until cols

    fun positions(): List<Pos> =
        (0 until rows).flatMap { r -> (0 until cols).map { c -> Pos(r, c) } }

    /** Returns a copy of this board with the given cells replaced. */
    fun with(changes: Map<Pos, Cell>): Board {
        if (changes.isEmpty()) return this
        val next = cells.toMutableList()
        for ((pos, cell) in changes) {
            require(pos in this) { "Position $pos outside board" }
            next[pos.row * cols + pos.col] = cell
        }
        return Board(rows, cols, next)
    }

    fun with(vararg changes: Pair<Pos, Cell>): Board = with(changes.toMap())

    /** All positions of one row, left to right. */
    fun rowPositions(row: Int): List<Pos> = (0 until cols).map { Pos(row, it) }

    /** All positions of one column, top to bottom. */
    fun colPositions(col: Int): List<Pos> = (0 until rows).map { Pos(it, col) }

    /** Serializes to a compact string: cells as tokens joined by ',', rows by ';'. */
    fun serialize(): String =
        (0 until rows).joinToString(";") { r ->
            (0 until cols).joinToString(",") { c -> this[Pos(r, c)].token() }
        }

    override fun equals(other: Any?): Boolean =
        other is Board && other.rows == rows && other.cols == cols && other.cells == cells

    override fun hashCode(): Int = 31 * (31 * rows + cols) + cells.hashCode()

    override fun toString(): String =
        (0 until rows).joinToString("\n") { r ->
            (0 until cols).joinToString("") { c -> this[Pos(r, c)].letter.toString() }
        }

    companion object {
        /**
         * Parses a level-definition grid: one string per row, one character per
         * cell. All rows must have equal length and contain only valid letters.
         */
        fun fromRows(rowStrings: List<String>): Board {
            require(rowStrings.isNotEmpty()) { "Level has no rows" }
            val cols = rowStrings.first().length
            require(cols > 0) { "Level has empty rows" }
            require(rowStrings.all { it.length == cols }) { "Level rows are not rectangular" }
            val cells = rowStrings.flatMap { row ->
                row.map { ch ->
                    require(Letters.isValidCell(ch)) { "Unknown letter '$ch' in level" }
                    Cell(ch)
                }
            }
            return Board(rowStrings.size, cols, cells)
        }

        /** Parses a grid given as a single string with '\n' between rows. */
        fun parse(text: String): Board = fromRows(text.trim().lines())

        fun deserialize(data: String): Board {
            val rows = data.split(";")
            val cells = rows.flatMap { row -> row.split(",").map(Cell::fromToken) }
            val cols = rows.first().split(",").size
            return Board(rows.size, cols, cells)
        }
    }
}
