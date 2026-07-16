package com.baldae.letterlab.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

/**
 * The examples in these tests come straight from the design document
 * (game_rules.txt) and the worked ASCII references in the original
 * implementation — they are the specification of the letter mechanics.
 */
class GameEngineTest {

    private fun board(text: String) = Board.parse(text.trimIndent())

    private fun move(board: Board, origin: Pos, target: Pos? = null): Board {
        val result = GameEngine.apply(board, origin, target)
        checkNotNull(result) { "Move from $origin to $target was rejected" }
        return result
    }

    // ------------------------------------------------------------ selection

    @Test
    fun `only action letters are selectable`() {
        val b = board("abcdefg\nhijklm_")
        val selectable = b.positions().filter { GameEngine.isSelectable(b, it) }
            .map { b[it].letter }.toSet()
        assertEquals(setOf('b', 'c', 'e', 'f', 'g', 'h', 'j', 'k', 'l', 'm'), selectable)
    }

    @Test
    fun `l and m act immediately on selection`() {
        val b = board("albma")
        assertIs<Selection.Immediate>(GameEngine.selectionFor(b, Pos(0, 1)))
        assertIs<Selection.Immediate>(GameEngine.selectionFor(b, Pos(0, 3)))
        assertIs<Selection.NeedsTarget>(GameEngine.selectionFor(b, Pos(0, 2)))
    }

    // -------------------------------------------------------------- b, c, j

    @Test
    fun `b swaps with an adjacent letter`() {
        // Doc: aba -> aab OR baa
        val b = board("aba")
        assertEquals(board("baa"), move(b, Pos(0, 1), Pos(0, 0)))
        assertEquals(board("aab"), move(b, Pos(0, 1), Pos(0, 2)))
    }

    @Test
    fun `c swaps exactly two slots away`() {
        // Doc: aacaa -> caaaa OR aaaac
        val b = board("aacaa")
        assertEquals(setOf(Pos(0, 0), Pos(0, 4)), GameEngine.targetsFor(b, Pos(0, 2)))
        assertEquals(board("caaaa"), move(b, Pos(0, 2), Pos(0, 0)))
    }

    @Test
    fun `j swaps exactly three slots away`() {
        // Doc: abcjcba -> jbcacba OR abcacbj
        val b = board("abcjcba")
        assertEquals(board("jbcacba"), move(b, Pos(0, 3), Pos(0, 0)))
        assertEquals(board("abcacbj"), move(b, Pos(0, 3), Pos(0, 6)))
    }

    @Test
    fun `swapping into a d deletes the swapper`() {
        // Doc: adb -> ad_  (b interacts with d and dies)
        val b = board("adb")
        assertEquals(board("ad_"), move(b, Pos(0, 2), Pos(0, 1)))
    }

    @Test
    fun `b can move into a blank`() {
        val b = board("b_a")
        assertEquals(board("_ba"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun `swap letters cannot target i`() {
        val b = board("bi")
        assertTrue(GameEngine.targetsFor(b, Pos(0, 0)).isEmpty())
        assertNull(GameEngine.apply(b, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun `swaps work vertically too`() {
        val b = board("b\na")
        assertEquals(board("a\nb"), move(b, Pos(0, 0), Pos(1, 0)))
    }

    // ------------------------------------------------------------------- e

    @Test
    fun `e eats an adjacent letter and becomes it`() {
        // Doc: abcec -> ab_cc  (e eats the c to its left)
        val b = board("abcec")
        assertEquals(board("ab_cc"), move(b, Pos(0, 3), Pos(0, 2)))
    }

    @Test
    fun `e can swallow a d whole`() {
        val b = board("ed")
        assertEquals(board("d_"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun `e that eats a blank destroys itself`() {
        val b = board("e_")
        assertEquals(board("__"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    // ------------------------------------------------------------------- f

    @Test
    fun `f shifts the grid with wraparound`() {
        val b = board("fab")
        assertEquals(board("bfa"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun `f shift right matches the reference example`() {
        // Original reference: a a d b a / d b c f b / a b c b a
        //          -> f> ->   a a d _ b / d _ b c f / a a b c b
        val b = board(
            """
            aadba
            dbcfb
            abcba
            """
        )
        val expected = board(
            """
            aad_b
            d_bcf
            aabcb
            """
        )
        assertEquals(expected, move(b, Pos(1, 3), Pos(1, 4)))
    }

    @Test
    fun `f shift left matches the reference example`() {
        // Original reference: a a d b a / d b c f b / a b c b a
        //          -> <f ->   a _ d a a / d c f b _ / b c b a a
        val b = board(
            """
            aadba
            dbcfb
            abcba
            """
        )
        val expected = board(
            """
            a_daa
            dcfb_
            bcbaa
            """
        )
        assertEquals(expected, move(b, Pos(1, 3), Pos(1, 2)))
    }

    @Test
    fun `f shifts vertically`() {
        val b = board("fa\nab\nbb")
        // Shift down: every cell moves one row down, bottom row wraps to top.
        assertEquals(board("bb\nfa\nab"), move(b, Pos(0, 0), Pos(1, 0)))
    }

    @Test
    fun `letters pass over i during a shift`() {
        val b = board("faib")
        // Shift right: b wraps to the front, a skips over the i.
        assertEquals(board("bfia"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    // ------------------------------------------------------------------- g

    @Test
    fun `g grabs a letter from its row and pulls it adjacent`() {
        // Doc: abcagbb -> bcaagbb OR abacgbb OR acabgbb
        val b = board("abcagbb")
        val g = Pos(0, 4)
        assertEquals(board("bcaagbb"), move(b, g, Pos(0, 0)))
        assertEquals(board("abacgbb"), move(b, g, Pos(0, 2)))
        assertEquals(board("acabgbb"), move(b, g, Pos(0, 1)))
    }

    @Test
    fun `g cannot grab blanks or i`() {
        val b = board("g_aib")
        val targets = GameEngine.targetsFor(b, Pos(0, 0))
        assertEquals(setOf(Pos(0, 2), Pos(0, 4)), targets)
    }

    @Test
    fun `grabbing a d drags it adjacent and wipes the path`() {
        val b = board("gabd")
        assertEquals(board("gd__"), move(b, Pos(0, 0), Pos(0, 3)))
    }

    @Test
    fun `grabbed letters pass over i`() {
        val b = board("g_ib")
        assertEquals(board("gbi_"), move(b, Pos(0, 0), Pos(0, 3)))
    }

    // ------------------------------------------------------------------- h

    @Test
    fun `h lifts a letter off the board`() {
        val b = board("ahb")
        val after = move(b, Pos(0, 1), Pos(0, 2))
        assertEquals(Cell('h', 'b'), after[Pos(0, 1)])
        assertEquals(Cell.EMPTY, after[Pos(0, 2)])
    }

    @Test
    fun `h swaps its held letter with a target`() {
        val b = board("ahb")
        val holding = move(b, Pos(0, 1), Pos(0, 2)) // h now holds b
        val after = move(holding, Pos(0, 1), Pos(0, 0)) // swap held b with the a
        assertEquals(Cell('b'), after[Pos(0, 0)])
        assertEquals(Cell('h', 'a'), after[Pos(0, 1)])
    }

    @Test
    fun `h can drop its held letter on a blank`() {
        val b = board("ahb")
        val holding = move(b, Pos(0, 1), Pos(0, 2))
        val after = move(holding, Pos(0, 1), Pos(0, 2)) // drop b back on the blank
        assertEquals(Cell('h'), after[Pos(0, 1)])
        assertEquals(Cell('b'), after[Pos(0, 2)])
    }

    @Test
    fun `empty-handed h cannot target blanks`() {
        val b = board("_h_")
        assertTrue(GameEngine.targetsFor(b, Pos(0, 1)).isEmpty())
    }

    @Test
    fun `h grabbing a d is destroyed and the d relocates`() {
        // Doc: if h grabs a d, h and its held letter die; d moves to h's cell.
        val b = board("ahd")
        val holding = move(b, Pos(0, 1), Pos(0, 0)) // h holds the a
        val after = move(holding, Pos(0, 1), Pos(0, 2))
        assertEquals(Cell('d'), after[Pos(0, 1)])
        assertEquals(Cell.EMPTY, after[Pos(0, 2)])
    }

    @Test
    fun `h has unlimited range but cannot hold another h`() {
        val b = board("b__\n__a\nh_h")
        val targets = GameEngine.targetsFor(b, Pos(2, 0))
        assertEquals(setOf(Pos(0, 0), Pos(1, 2)), targets)
    }

    // ------------------------------------------------------------------- k

    @Test
    fun `k kicks an adjacent letter to the edge`() {
        // Doc: bkabba -> bkbbaa
        val b = board("bkabba")
        assertEquals(board("bkbbaa"), move(b, Pos(0, 1), Pos(0, 2)))
    }

    @Test
    fun `k cannot kick blanks or i`() {
        val b = board("_ki")
        assertTrue(GameEngine.targetsFor(b, Pos(0, 1)).isEmpty())
    }

    @Test
    fun `kicking a d boots it to the edge and wipes the path`() {
        val b = board("kdab")
        assertEquals(board("k__d"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    // ------------------------------------------------------------------- l

    @Test
    fun `l kicks every cardinal neighbour to the edges`() {
        // Doc: bacblccbb -> bbaclcbbc
        val b = board("bacblccbb")
        assertEquals(board("bbaclcbbc"), move(b, Pos(0, 4)))
    }

    // ------------------------------------------------------------------- m

    @Test
    fun `m grabs the edge letters inward`() {
        // Doc: bacbmccbb -> acbbmbccb
        val b = board("bacbmccbb")
        assertEquals(board("acbbmbccb"), move(b, Pos(0, 4)))
    }

    @Test
    fun `m works on non-square grids`() {
        // Regression: the original implementation had a bottom-edge bug on
        // non-square boards.
        val b = board("aba\nama\naba\nacd")
        val after = move(b, Pos(1, 1))
        // Column 1 below m: edge is (3,1) 'c'; it gets pulled adjacent to m at
        // (2,1) and the old (2,1) 'b' shifts down toward the edge.
        assertEquals('c', after[Pos(2, 1)].letter)
        assertEquals('b', after[Pos(3, 1)].letter)
    }

    // ------------------------------------------------------------ win check

    @Test
    fun `rows and columns sorted either direction are solved`() {
        assertTrue(GameEngine.isSolved(board("abc")))
        assertTrue(GameEngine.isSolved(board("cba")))
        assertTrue(GameEngine.isSolved(board("ab\nab")))
        assertTrue(GameEngine.isSolved(board("ab\nba"))) // asc row, desc row, monotone cols
        assertTrue(GameEngine.isSolved(board("aabbb")))
    }

    @Test
    fun `blanks are ignored by the win check`() {
        assertTrue(GameEngine.isSolved(board("a_b")))
        assertTrue(GameEngine.isSolved(board("c_a")))
    }

    @Test
    fun `unsorted lines are not solved`() {
        assertFalse(GameEngine.isSolved(board("aba")))
        assertFalse(GameEngine.isSolved(board("bab")))
        assertFalse(GameEngine.isSolved(board("ab\nba\nab"))) // middle column b,a,b
    }

    @Test
    fun `an h counts as the letter h when checking the win`() {
        // a,h,i is sorted; if the held 'm' counted instead, a,m,i would not be.
        val b = board("ahi").with(Pos(0, 1) to Cell('h', 'm'))
        assertTrue(GameEngine.isSolved(b))
    }

    // ------------------------------------------------------- h interactions

    @Test
    fun `a swap moves an h together with its held letter`() {
        val b = board("bha").with(Pos(0, 1) to Cell('h', 'c'))
        val after = move(b, Pos(0, 0), Pos(0, 1))
        assertEquals(Cell('h', 'c'), after[Pos(0, 0)])
        assertEquals(Cell('b'), after[Pos(0, 1)])
    }

    @Test
    fun `e that eats an h swallows the held letter too`() {
        val b = board("eha").with(Pos(0, 1) to Cell('h', 'c'))
        val after = move(b, Pos(0, 0), Pos(0, 1))
        assertEquals(Cell('h', 'c'), after[Pos(0, 0)])
        assertEquals(Cell.EMPTY, after[Pos(0, 1)])
    }

    @Test
    fun `a shift carries an h with its held letter`() {
        val b = board("fha").with(Pos(0, 1) to Cell('h', 'c'))
        val after = move(b, Pos(0, 0), Pos(0, 1)) // shift right
        assertEquals(Cell('h', 'c'), after[Pos(0, 2)])
    }

    // --------------------------------------------------------------- board

    @Test
    fun `board serialization round-trips including held letters`() {
        val b = board("ahb").with(Pos(0, 1) to Cell('h', 'b'))
        val restored = Board.deserialize(b.serialize())
        assertEquals(b, restored)
        assertEquals(Cell('h', 'b'), restored[Pos(0, 1)])
    }

    @Test
    fun `illegal moves are rejected`() {
        val b = board("ba")
        assertNull(GameEngine.apply(b, Pos(0, 1), Pos(0, 0))) // 'a' is not selectable
        assertNull(GameEngine.apply(b, Pos(0, 0), Pos(0, 0))) // self is not a target
    }
}
