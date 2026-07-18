package com.baldae.letterlab.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

/**
 * Mechanics of the letters n–z, from the worked examples in
 * docs/game_rules.txt (adapted from LetterLab_FinalProjectFall2024).
 */
class GameEngineNzTest {

    private fun board(text: String) = Board.parse(text.trimIndent())

    private fun move(board: Board, origin: Pos, target: Pos? = null): Board {
        val result = GameEngine.apply(board, origin, target)
        checkNotNull(result) { "Move from $origin to $target was rejected" }
        return result
    }

    // -------------------------------------------------------------- n nullify

    @Test
    fun `a letter next to an n cannot act`() {
        val b = board("bna")
        assertIs<Selection.NotSelectable>(GameEngine.selectionFor(b, Pos(0, 0)))
    }

    @Test
    fun `a letter one step past the aura still acts`() {
        val b = board("bana")
        assertIs<Selection.NeedsTarget>(GameEngine.selectionFor(b, Pos(0, 0)))
    }

    @Test
    fun `two adjacent n's switch each other off`() {
        // Doc: no negative stacking; adjacent n's cancel.
        val b = board("bnna")
        assertIs<Selection.NeedsTarget>(GameEngine.selectionFor(b, Pos(0, 0)))
    }

    @Test
    fun `three n's cancel just like two`() {
        val b = board("bnnna")
        assertIs<Selection.NeedsTarget>(GameEngine.selectionFor(b, Pos(0, 0)))
    }

    @Test
    fun `a nullified d neither deletes nor stands firm`() {
        // The d next to n behaves like an 'a': b swaps with it and lives.
        val b = board("bdn")
        assertEquals(board("dbn"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun `a nullified i can finally be touched`() {
        val b = board("bin")
        assertEquals(board("ibn"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun `a protected i still cannot be touched`() {
        val b = board("bia")
        assertTrue(Pos(0, 1) !in GameEngine.targetsFor(b, Pos(0, 0)))
    }

    @Test
    fun `n itself can be moved like any letter`() {
        // c reaches past the aura (range 2) and swaps with the n itself.
        val b = board("can")
        assertEquals(board("nac"), move(b, Pos(0, 0), Pos(0, 2)))
    }

    // -------------------------------------------------- o onomatopoeia

    @Test
    fun `o swaps with any vowel at any distance`() {
        // Doc: abaobba -> obaabba OR aboabba OR abaabbo
        val b = board("abaobba")
        assertEquals(board("obaabba"), move(b, Pos(0, 3), Pos(0, 0)))
        assertEquals(board("abaabbo"), move(b, Pos(0, 3), Pos(0, 6)))
    }

    @Test
    fun `o cannot reach consonants or i`() {
        val b = board("obi")
        assertEquals(emptySet(), GameEngine.targetsFor(b, Pos(0, 0)))
    }

    @Test
    fun `o reaches an e and another o`() {
        val b = board("oaeo")
        assertEquals(
            setOf(Pos(0, 1), Pos(0, 2), Pos(0, 3)),
            GameEngine.targetsFor(b, Pos(0, 0)),
        )
    }

    // ---------------------------------------------------------------- p purge

    @Test
    fun `p fires immediately and destroys itself and its neighbours`() {
        val b = board("apd")
        assertIs<Selection.Immediate>(GameEngine.selectionFor(b, Pos(0, 1)))
        assertEquals(board("___"), move(b, Pos(0, 1)))
    }

    @Test
    fun `p destroys a d — the only letter that can`() {
        val b = board("bpd")
        assertEquals(board("___"), move(b, Pos(0, 1)))
    }

    @Test
    fun `only an i survives the purge`() {
        val b = board("ipb")
        assertEquals(board("i__"), move(b, Pos(0, 1)))
    }

    @Test
    fun `purge is cardinal — diagonals survive`() {
        val b = board("""
            bab
            apa
            bab
        """)
        assertEquals(
            board("""
                b_b
                ___
                b_b
            """),
            move(b, Pos(1, 1)),
        )
    }

    // ---------------------------------------------------------------- q queen

    @Test
    fun `q swaps any distance along row column and diagonal`() {
        val b = board("""
            baaab
            aaaaa
            aaqaa
            aaaaa
            baaab
        """)
        val targets = GameEngine.targetsFor(b, Pos(2, 2))
        // Full row, column, and both diagonals: corners included.
        assertTrue(Pos(0, 0) in targets)
        assertTrue(Pos(4, 4) in targets)
        assertTrue(Pos(0, 4) in targets)
        assertTrue(Pos(2, 0) in targets)
        assertTrue(Pos(0, 2) in targets)
        // Off-line knight-ish cell excluded.
        assertFalse(Pos(0, 1) in targets)
    }

    @Test
    fun `q dies touching a d like everyone else`() {
        val b = board("qad")
        assertEquals(board("_ad"), move(b, Pos(0, 0), Pos(0, 2)))
    }

    // --------------------------------------------------------------- r remove

    @Test
    fun `r removes every copy of the adjacent letter and becomes an a`() {
        // Doc: accdrca (select the adjacent c) -> a__dr_a, r -> a
        val b = board("accdrca")
        assertEquals(board("a__da_a"), move(b, Pos(0, 4), Pos(0, 5)))
    }

    @Test
    fun `r removes all d's at the cost of itself`() {
        val b = board("rdadb")
        assertEquals(board("__a_b"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun `r cannot target an i or a blank`() {
        val b = board("ri_b")
        assertEquals(emptySet(), GameEngine.targetsFor(b, Pos(0, 0)))
    }

    @Test
    fun `r removing r's removes itself too`() {
        val b = board("rrb")
        assertEquals(board("__b"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    // ----------------------------------------------------------------- s spin

    @Test
    fun `s swaps its two opposite neighbours`() {
        // Doc: bsa -> asb
        val b = board("bsa")
        assertEquals(board("asb"), move(b, Pos(0, 1), Pos(0, 0)))
        // Selecting either end gives the same result.
        assertEquals(board("asb"), move(b, Pos(0, 1), Pos(0, 2)))
    }

    @Test
    fun `spinning into a d destroys the crossing letter`() {
        val b = board("dsb")
        assertEquals(board("ds_"), move(b, Pos(0, 1), Pos(0, 2)))
    }

    @Test
    fun `s works vertically too`() {
        val b = board("b\ns\na")
        assertEquals(board("a\ns\nb"), move(b, Pos(1, 0), Pos(0, 0)))
    }

    @Test
    fun `s at the edge has no opposite and no target that way`() {
        val b = board("sa_")
        assertEquals(emptySet(), GameEngine.targetsFor(b, Pos(0, 0)))
    }

    // --------------------------------------------------------------- t toggle

    @Test
    fun `t turns an adjacent letter into the previous one`() {
        // Doc: btc (select the b) -> atc
        val b = board("btc")
        assertEquals(board("atc"), move(b, Pos(0, 1), Pos(0, 0)))
        assertEquals(board("btb"), move(b, Pos(0, 1), Pos(0, 2)))
    }

    @Test
    fun `toggling an a wraps to z`() {
        val b = board("at")
        assertEquals(board("zt"), move(b, Pos(0, 1), Pos(0, 0)))
    }

    @Test
    fun `toggling a d is suicide`() {
        val b = board("td")
        assertEquals(board("_d"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    // -------------------------------------------------------------- u upgrade

    @Test
    fun `u turns an adjacent letter into the next one`() {
        // Doc: buc (select the b) -> cuc
        val b = board("buc")
        assertEquals(board("cuc"), move(b, Pos(0, 1), Pos(0, 0)))
    }

    @Test
    fun `upgrading a z wraps to a`() {
        val b = board("zu")
        assertEquals(board("au"), move(b, Pos(0, 1), Pos(0, 0)))
    }

    // ---------------------------------------------------------------- v virus

    @Test
    fun `v swaps like b and infects its partner`() {
        // Doc: abvac (select the b) -> avvac
        val b = board("abvac")
        assertEquals(board("avvac"), move(b, Pos(0, 2), Pos(0, 1)))
    }

    @Test
    fun `v moving into a blank infects nothing`() {
        val b = board("v_a")
        assertEquals(board("_va"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun `a d destroys the v but catches the virus`() {
        val b = board("vd")
        assertEquals(board("_v"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun `v cannot touch an i`() {
        val b = board("vi")
        assertEquals(emptySet(), GameEngine.targetsFor(b, Pos(0, 0)))
    }

    // ---------------------------------------------------------------- w waltz

    @Test
    fun `w swaps with a diagonal neighbour`() {
        val b = board("wa\nab")
        assertEquals(board("ba\naw"), move(b, Pos(0, 0), Pos(1, 1)))
    }

    @Test
    fun `w cannot move cardinally`() {
        val b = board("wa\nab")
        assertEquals(setOf(Pos(1, 1)), GameEngine.targetsFor(b, Pos(0, 0)))
    }

    @Test
    fun `a d on the diagonal is still fatal`() {
        val b = board("wa\nad")
        assertEquals(board("_a\nad"), move(b, Pos(0, 0), Pos(1, 1)))
    }

    // ---------------------------------------------------------------- x xerox

    @Test
    fun `x becomes a copy of an adjacent letter`() {
        // Doc: caxb (select the b) -> cabb
        val b = board("caxb")
        assertEquals(board("cabb"), move(b, Pos(0, 2), Pos(0, 3)))
    }

    @Test
    fun `x can photocopy a d safely`() {
        val b = board("xd")
        assertEquals(board("dd"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun `x copies an h with its held letter`() {
        val start = Board(1, 2, listOf(Cell('x'), Cell('h', 'b')))
        val result = move(start, Pos(0, 0), Pos(0, 1))
        assertEquals(Cell('h', 'b'), result[Pos(0, 0)])
    }

    @Test
    fun `x cannot copy an i a blank or another x`() {
        assertEquals(emptySet(), GameEngine.targetsFor(board("_xi"), Pos(0, 1)))
        assertEquals(setOf(Pos(0, 0)), GameEngine.targetsFor(board("axx"), Pos(0, 1)))
    }

    // ----------------------------------------------------------------- y yeet

    @Test
    fun `y hurls itself to the far edge and the rest shift back`() {
        val b = board("yaab")
        assertEquals(board("aaby"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun `y sails over an i`() {
        // The i keeps its cell; y still reaches the edge, a and b shift back.
        val b = board("yaib")
        assertEquals(board("abiy"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun `yeeting into an edge d is fatal`() {
        val b = board("yad")
        assertEquals(board("a_d"), move(b, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun `y at the wall cannot yeet into it`() {
        val b = board("ya")
        assertEquals(setOf(Pos(0, 1)), GameEngine.targetsFor(b, Pos(0, 0)))
    }

    // ------------------------------------------------------------------ z zap

    @Test
    fun `z swaps with any cell anywhere`() {
        val b = board("""
            zaa
            aaa
            aab
        """)
        assertEquals(
            board("""
                baa
                aaa
                aaz
            """),
            move(b, Pos(0, 0), Pos(2, 2)),
        )
    }

    @Test
    fun `z reaches everything but i`() {
        val b = board("zai\nbcd")
        val targets = GameEngine.targetsFor(b, Pos(0, 0))
        assertEquals(b.positions().size - 2, targets.size)
        assertFalse(Pos(0, 2) in targets)
    }

    @Test
    fun `z dies to a d like everyone else`() {
        val b = board("zad")
        assertEquals(board("_ad"), move(b, Pos(0, 0), Pos(0, 2)))
    }

    // ------------------------------------------------- solving with new letters

    @Test
    fun `win check uses real letters even under an aura`() {
        // "bn" is sorted ascending; the aura doesn't change sorting.
        assertTrue(GameEngine.isSolved(board("bn")))
        assertFalse(GameEngine.isSolved(board("ban")))
    }

    @Test
    fun `illegal targets are rejected`() {
        assertNull(GameEngine.apply(board("oba"), Pos(0, 0), Pos(0, 1)))
        assertNull(GameEngine.apply(board("ri_"), Pos(0, 0), Pos(0, 1)))
        assertNull(GameEngine.apply(board("wa\nab"), Pos(0, 0), Pos(0, 1)))
    }
}
