package com.baldae.letterlab.domain.solver

import com.baldae.letterlab.domain.Board
import com.baldae.letterlab.domain.GameEngine
import org.junit.Test

/**
 * Scratch tooling for authoring levels: solves candidate grids with
 * exhaustive BFS and prints optimal solutions as letter@r,c -> r,c chains,
 * so a designer can check a candidate is solvable, appropriately deep, and
 * actually exercises the intended mechanic. Not a gate; edited freely.
 */
class LevelSandboxTest {

    private fun analyze(label: String, vararg rows: String) {
        val board = Board.fromRows(rows.toList())
        if (GameEngine.isSolved(board)) {
            println("$label: PRE-SOLVED")
            return
        }
        val solver = Solver(bfsNodeBudget = 500_000, beamWidth = 400, maxBeamMoves = 60)
        // Exhaustive BFS only where the state space stays tractable; boards
        // with global-reach letters (o/q/z/h) explode the branching factor.
        val heavy = board.positions().any { board[it].letter in "oqzh" }
        val cells = board.rows * board.cols
        var result: Solver.Result =
            if (cells <= 12 || (cells <= 20 && !heavy)) solver.solveOptimal(board)
            else Solver.Result.Unknown
        if (result is Solver.Result.Unknown) {
            for (w in listOf(1.5, 2.5, 4.0)) {
                result = solver.solveBestFirst(board, w, nodeBudget = 60_000)
                if (result !is Solver.Result.Unknown) break
            }
        }
        if (result is Solver.Result.Unknown) result = solver.solveAny(board)
        when (result) {
            is Solver.Result.Solved -> {
                val steps = mutableListOf<String>()
                var replay = board
                for (m in result.moves) {
                    val letter = GameEngine.effective(replay, m.origin)
                    steps += "$letter@${m.origin.row},${m.origin.col}" +
                        (m.target?.let { "->${it.row},${it.col}" } ?: "")
                    replay = GameEngine.apply(replay, m.origin, m.target) ?: break
                }
                println("$label: ${result.moves.size} moves (optimal=${result.optimal}): " +
                    steps.joinToString("  "))
            }
            Solver.Result.ProvenUnsolvable -> println("$label: UNSOLVABLE")
            Solver.Result.Unknown -> println("$label: unknown (budget)")
        }
    }

    @org.junit.Ignore("Scratch tooling: add candidate grids and remove @Ignore while authoring levels.")
    @Test
    fun `analyze round 5`() {
        // Tutorial fixes: strictly letter-forced (only passive fillers a/d/i/n)
        analyze("h-tut", "ahd")
        analyze("k-tut", "kdaan", "aaaad")
        analyze("u-tut", "uac")
        analyze("s-tut FIX", "a_d", "_sa", "d_i")
        analyze("v-tut", "avda")
        // W7 deepening: 53 Pattern Purge / 54 Scorched Earth / 56 Derby
        analyze("53 A", "rbcb", "cbcb", "bcbc")
        analyze("53 B", "brcb", "cbbc", "bccb")
        analyze("54 A", "rdbc", "dbdb", "bdbc")
        analyze("54 B", "crdb", "dbdc", "bdcb")
        analyze("56 B", "kpdc", "rabc", "bcba", "acbd", "bacb")
        // W8: 63 Forgery deeper
        analyze("63 D", "axtb", "_c_a", "_cbb")
        analyze("63 E", "axt", "bc_", "_cb")
        // W10: 74 Royal Court deeper
        analyze("74 D", "d_ca", "_aq_", "a_cd")
        analyze("74 E", "a_cb", "_aq_", "b__a")
        analyze("74 F", "dca", "_q_", "acd")
    }

    @org.junit.Ignore("Scratch tooling: add candidate grids and remove @Ignore while authoring levels.")
    @Test
    fun `analyze round 6`() {
        // 53: removal alone must not solve; a setup/cleanup move required
        analyze("53 C", "rbcb", "adca", "bacb")
        analyze("53 D", "rcbc", "adca", "cabc")
        // 56: capstone deeper
        analyze("56 C", "kpdcb", "rabcb", "bcbac", "acbdc")
        // 74: dual-flaw single royal move
        analyze("74 G", "c_d", "dqa", "a_a")
    }

    @org.junit.Ignore("Scratch tooling: add candidate grids and remove @Ignore while authoring levels.")
    @Test
    fun `analyze round 3`() {
        // 42 Off Switch: forced nullified-d swap
        analyze("42 LOCK", "bda", "bn_", "a_b")
        // 47 Hush Money: o + n with more depth
        analyze("47 A", "o_bn", "aab_", "bab_")
        analyze("47 B", "oabn", "_ab_", "bab_")
        // 16 Meltdown: e+f+d capstone, deeper
        analyze("16 A", "bedf", "abca", "cbab", "dcba")
        analyze("16 B", "edfb", "bacb", "abca", "cbad")
        // 50 Controlled Burn: purge with setup
        analyze("50 A", "apdb", "bcab", "acba")
        analyze("50 B", "dpab", "abca", "bacb")
        // 55 Dust and Ashes / 56 Demolition Derby: bigger
        analyze("55 A", "prcb", "cbcb", "bcbc")
        analyze("55 B", "prdc", "cbcb", "bccb")
        analyze("56 A", "kpdc", "rabc", "bcba", "acbd")
        analyze("56 B", "kpdb", "rcbc", "cbcb", "bcbc")
        // 63 Forgery: x must copy
        analyze("63 A", "axc", "_tb", "__b")
        analyze("63 B", "axt", "_c_", "_cb")
        analyze("63 C", "ax", "_a", "_b")
        // 66 Pirouette: two spins
        analyze("66 LOCK", "a_d", "_sa", "_sb", "d_c")
        // 68 Ballroom: two waltz hops
        analyze("68 LOCK", "wad", "_a_", "ada")
        // 71 Dance Card: s+w+y
        analyze("71 A", "yad", "as_", "_wd")
        analyze("71 B", "y_ad", "as_a", "_w_d")
        analyze("71 C", "s_ya", "aw_d", "d__c")
        // 74 Royal Court / 78 Lightning Round
        analyze("74 A", "a_c", "_aq", "__a")
        analyze("74 B", "a_ca", "_aq_", "__ad")
        analyze("74 C", "d_ca", "_aqa", "a__d")
        analyze("78 A", "azd", "_a_", "d__")
        analyze("78 B", "azdc", "_a_c", "d__d")
    }

    @org.junit.Ignore("Scratch tooling: add candidate grids and remove @Ignore while authoring levels.")
    @Test
    fun `analyze round 4`() {
        analyze("66 FIX", "a_d", "_sa", "_sb", "d_d")
        analyze("78 FIX", "azd", "_a_", "dza")
    }

    @org.junit.Ignore("Scratch tooling: add candidate grids and remove @Ignore while authoring levels.")
    @Test
    fun `analyze candidates`() {
        // --- locked tutorial designs: confirm move count + letter used ---
        analyze("n-tut LOCK", "bia", "bn_", "a_b")
        analyze("s-tut LOCK", "a_d", "_sa", "d_c")
        analyze("w-tut LOCK", "awd", "_a_")
        analyze("q-tut LOCK", "qa_", "_d_", "ada")
        analyze("t-tut LOCK", "ata")
        analyze("u-tut LOCK", "uab")
        analyze("x-tut LOCK", "cxc")

        // --- Off Switch (42): trace — does it exploit the nullified d? ---
        analyze("42 current", "bdca", "naba")

        // --- Double Negative (44): adjacent n's cancel; blanks free the layout ---
        analyze("nn A", "bnna", "ab__")
        analyze("nn B", "bnn", "a_a", "bab")
        analyze("nn C", "cnnb", "a__b")
        analyze("nn D", "bnn_", "a__b")

        // --- Hush Money (47) n+o with blanks ---
        analyze("no A", "o_n", "a_b", "abb")
        analyze("no B", "oa_n", "_ab_")
        analyze("no C", "o_bn", "aab_")
        analyze("no D", "oan", "_b_", "abb")

        // --- Sound and Silence (48): n + o + i capstone ---
        analyze("ns A", "bi_n", "oa_b", "ab_b")
        analyze("ns B", "oib", "_n_", "abb")
        analyze("ns C", "oi_n", "_a_b", "ab_b")
        analyze("ns D", "ion", "b_a", "bab")

        // --- Bank Shot (27): i intro — kick must sail over the i ---
        analyze("ki A", "kbia", "abca")
        analyze("ki B", "kbi_a")
        analyze("ki C", "kbica", "ab_ba")
        analyze("ki D", "kaiba", "ba_ab")

        // --- Wheel Work (13): deeper f ---
        analyze("f A", "fba", "bab", "bba")
        analyze("f B", "afb", "bab", "abb")
        analyze("f C", "fabb", "babb", "abba")

        // --- Conveyor (15) e+f / Meltdown (16) capstone ---
        analyze("ef A", "efab", "abba", "babb")
        analyze("ef B", "eafb", "abab", "bbaa")
        analyze("efd A", "bedf", "abca", "cbaa")
        analyze("efd B", "edfb", "bacb", "abca")

        // --- Full Bloom (29): l with setup ---
        analyze("l A", "balb", "abab", "baba")
        analyze("l B", "blab", "aabb", "bbaa")
        analyze("l C", "alba", "baab", "abba")

        // --- Half Alphabet (40): trace the 1-move cheese ---
        analyze("40 current", "abcd", "efgh", "jklm", "i___")
        analyze("40 B", "badc", "fehg", "kjml", "i___")
        analyze("40 C", "mlkj", "hgfe", "dcba", "___i")

        // --- Controlled Burn (50): p needs setup first ---
        analyze("p A", "dpa", "bab", "aba")
        analyze("p B", "bpd", "cab", "bca")
        analyze("p C", "apcb", "bdab")

        // --- Dust and Ashes (55) / Demolition Derby (56) ---
        analyze("pr A", "prcb", "cbcb")
        analyze("pr B", "prc", "cbc", "bcb")
        analyze("prk A", "kpdc", "rabc", "bcba")
        analyze("prk B", "kpd", "rcb", "cbc")

        // --- Alphabet Soup (58): t depth / Forgery (63): x+t ---
        analyze("t A", "atac", "bbca")
        analyze("t B", "cta", "bab", "abc")
        analyze("t C", "atab", "cbab")
        analyze("xt A", "xta", "bcb", "aca")
        analyze("xt B", "txc", "aab", "bba")

        // --- W9/W10 beef: Pirouette(66) Ballroom(68) Dance Card(71) Ballet(72) ---
        analyze("s2 A", "asc", "b_b", "csa")
        analyze("w2 A", "wab", "a_a", "baw")
        analyze("swy A", "s_y", "aw_", "bab")
        analyze("swy B", "was", "b_c", "cby")

        // --- Royal Court (74) / Lightning (78) / Full Alphabet (79) / Ascension (80) ---
        analyze("q2 A", "qba", "b_b", "abd")
        analyze("z2 A", "zbc", "c_b", "bcd")
        analyze("nz A", "orst", "u_yz", "npqw")
        analyze("80 cur", "bqcvb", "waiaz", "cbsbc", "yanat", "bucpb")
    }
}
