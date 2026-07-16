package com.baldae.letterlab.domain

/**
 * Central registry of every letter in the LetterLab alphabet.
 *
 * Semantics follow the design document (game_rules.txt from the original
 * project), which is the source of truth for the intended mechanics:
 *
 * - `a` Apathetic  — does nothing, but other letters can move it.
 * - `b` Basic      — swaps with a cardinally adjacent letter.
 * - `c` Complex    — swaps with a letter exactly two slots away, cardinally.
 * - `d` Delete     — deletes whatever interacts with it; never moves on its own.
 * - `e` Eat        — consumes an adjacent cell and becomes its content.
 * - `f` Force      — shifts the whole grid one step, wrapping at the edges.
 * - `g` Grab       — pulls a letter in its row/column adjacent to itself.
 * - `h` Hold       — lifts a letter off the board and can swap it back anywhere.
 * - `i` Immovable  — cannot be interacted with or moved at all.
 * - `j` Jump       — swaps with a letter exactly three slots away, cardinally.
 * - `k` Kick       — boots an adjacent letter to the edge of the board.
 * - `l` Lovely     — kicks every cardinal neighbour to the edges when selected.
 * - `m` Monstrous  — grabs the four edge letters inward when selected.
 */
object Letters {
    const val EMPTY = '_'

    /** Every valid letter that may appear in a level definition. */
    val ALL: Set<Char> = "abcdefghijklm".toSet()

    /** Letters the player can pick up and use to act on the board. */
    val SELECTABLE: Set<Char> = setOf('b', 'c', 'e', 'f', 'g', 'h', 'j', 'k', 'l', 'm')

    /** Letters that fire immediately when selected, without picking a target. */
    val IMMEDIATE: Set<Char> = setOf('l', 'm')

    /** Swap distance for the swap-family letters, or null for the rest. */
    fun swapRange(letter: Char): Int? = when (letter) {
        'b' -> 1
        'c' -> 2
        'j' -> 3
        else -> null
    }

    fun isValidCell(c: Char): Boolean = c == EMPTY || c in ALL
}
