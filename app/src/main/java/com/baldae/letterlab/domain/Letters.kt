package com.baldae.letterlab.domain

/**
 * Central registry of every letter in the LetterLab alphabet.
 *
 * Semantics follow the design document (docs/game_rules.txt, adapted from
 * LetterLab_FinalProjectFall2024), which is the source of truth:
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
 * - `n` Nullify    — passive aura: cardinal neighbours act like 'a'. Two
 *                    adjacent n's switch each other's auras off.
 * - `o` Onomatopoeia — swaps with any vowel (a, e, o, u) anywhere on the board.
 * - `p` Purge      — fires on selection: destroys itself and its four
 *                    neighbours. The only letter that can destroy a d.
 * - `q` Queen      — swaps any distance along its row, column, or diagonals.
 * - `r` Remove     — removes every copy of an adjacent letter, then becomes 'a'.
 * - `s` Spin       — swaps its two opposite neighbours through itself.
 * - `t` Toggle     — turns an adjacent letter into the previous letter (a→z).
 * - `u` Upgrade    — turns an adjacent letter into the next letter (z→a).
 * - `v` Virus      — swaps like b, but the letter it swaps with becomes a v.
 * - `w` Waltz      — swaps with a diagonally adjacent letter.
 * - `x` Xerox      — becomes an exact copy of an adjacent letter.
 * - `y` Yeet       — hurls itself to the far edge; passed letters shift back.
 * - `z` Zap        — swaps with any cell anywhere on the board.
 */
object Letters {
    const val EMPTY = '_'

    /** Every valid letter that may appear in a level definition. */
    val ALL: Set<Char> = ('a'..'z').toSet()

    /** Letters the player can pick up and use to act on the board. */
    val SELECTABLE: Set<Char> =
        setOf('b', 'c', 'e', 'f', 'g', 'h', 'j', 'k', 'l', 'm') +
            setOf('o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z')

    /** Letters that fire immediately when selected, without picking a target. */
    val IMMEDIATE: Set<Char> = setOf('l', 'm', 'p')

    /** The vowels o can swap with. i is beyond everything's reach. */
    val VOWELS: Set<Char> = setOf('a', 'e', 'o', 'u')

    /** Swap distance for the swap-family letters, or null for the rest. */
    fun swapRange(letter: Char): Int? = when (letter) {
        'b' -> 1
        'c' -> 2
        'j' -> 3
        else -> null
    }

    /** The previous letter alphabetically, wrapping a → z (t's toggle). */
    fun previous(letter: Char): Char = if (letter == 'a') 'z' else letter - 1

    /** The next letter alphabetically, wrapping z → a (u's upgrade). */
    fun next(letter: Char): Char = if (letter == 'z') 'a' else letter + 1

    fun isValidCell(c: Char): Boolean = c == EMPTY || c in ALL
}
