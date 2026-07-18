package com.baldae.letterlab.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test

class SaveMigrationTest {

    private fun done(id: Int) = booleanPreferencesKey("done_$id")
    private fun best(id: Int) = intPreferencesKey("best_$id")
    private fun stars(id: Int) = intPreferencesKey("stars_$id")
    private fun save(id: Int) = stringPreferencesKey("save_$id")
    private fun tut(id: Int) = booleanPreferencesKey("tut_$id")

    /** The v1 campaign grids, verbatim, as shipped before the A–Z expansion. */
    private val v1Grids: Map<Int, List<String>> = mapOf(
        1 to listOf("aabab"),
        2 to listOf("aabab", "aabab"),
        3 to listOf("ababa", "bacab", "bbcbb"),
        4 to listOf("aba", "bcb", "aba"),
        5 to listOf("abac", "cdab"),
        6 to listOf("ccbcc", "bbabb", "cadac", "bcacb"),
        7 to listOf("abce", "cbca"),
        8 to listOf("bcb", "eae", "bcb"),
        9 to listOf("abbab", "abbab", "aafbb", "babba", "babba"),
        10 to listOf("abdb", "dbca", "abcf"),
        11 to listOf("gaabcab", "bcbdaag"),
        12 to listOf("aaab", "baaa", "abaa", "aaba", "gccg"),
        13 to listOf("bac", "bbb", "ghc", "aca", "bbc"),
        14 to listOf("bbcb", "bheb", "bcdb", "eaae"),
        15 to listOf("jcbcj", "cbabc", "bajab", "cbacb", "jcbcj"),
        16 to listOf("facaj", "edade", "jacaf"),
        17 to listOf("kcbc", "bcab", "jeje", "cbck"),
        18 to listOf("kdefk", "jcbfk"),
        19 to listOf(
            "bdbcdbd", "dbdbbdb", "bdbadbd", "cbalabc", "dbdabdb", "bdbbdbd", "dbdcbdb"
        ),
        20 to listOf("ljjjaah", "ekfgbba", "aaaabbc"),
        21 to listOf(
            "dbdcbdb", "bdbbdbd", "dbdabdb", "cbamabc", "bdbadbd", "dbdbbdb", "bdbcdbd"
        ),
        22 to listOf("aabbc", "abcde", "bcdee", "bmebb", "aaaaa"),
        23 to listOf("gaaaaad", "kdaaaaa", "abafaba"),
        24 to listOf("abcd", "efgh", "jklm", "hgfe", "dcba"),
        25 to listOf("ggaaagg", "deeeeed", "hkhkhkh"),
    )

    @Test
    fun `every mapped level id exists in the v2 catalog`() {
        val catalog = LevelRepository.parse(File("src/main/assets/levels.json").readText())
        for ((old, new) in SaveMigration.OLD_TO_NEW) {
            assertTrue(catalog.level(new) != null, "old $old maps to missing level $new")
        }
        // The mapping is injective: no two old levels collapse onto one.
        assertEquals(SaveMigration.OLD_TO_NEW.size, SaveMigration.OLD_TO_NEW.values.toSet().size)
    }

    @Test
    fun `carried-over best moves and snapshots only apply to identical grids`() {
        val catalog = LevelRepository.parse(File("src/main/assets/levels.json").readText())
        for ((old, new) in SaveMigration.OLD_TO_NEW) {
            if (old in SaveMigration.REDESIGNED_OLD_IDS) continue
            assertEquals(
                v1Grids.getValue(old), catalog.level(new)!!.grid,
                "old level $old carried best/snapshot onto a changed grid (new $new)"
            )
        }
        // And the redesigned set is accurate: those grids really did change.
        for (old in SaveMigration.REDESIGNED_OLD_IDS) {
            val new = SaveMigration.OLD_TO_NEW.getValue(old)
            assertFalse(
                v1Grids.getValue(old) == catalog.level(new)!!.grid,
                "old level $old is marked redesigned but its grid is unchanged"
            )
        }
    }

    @Test
    fun `representative v1 save migrates completely`() {
        val old = preferencesOf(
            // Player beat levels 1-7, some stars, a best on 5 and 7.
            done(1) to true, stars(1) to 3, best(1) to 1,
            done(2) to true, stars(2) to 2, best(2) to 3,
            done(5) to true, stars(5) to 3, best(5) to 4,
            done(6) to true, stars(6) to 1, best(6) to 15,
            done(7) to true, stars(7) to 3, best(7) to 1,
            // Mid-level snapshot on old 9 (Full Force, unchanged grid).
            save(9) to "a,b,b,a,b;a,b,b,a,b;a,a,f,b,b;b,a,b,b,a;b,a,b,b,a|2",
            // Seen tutorials on old 1 and 5.
            tut(1) to true, tut(5) to true,
            // Level-independent data.
            intPreferencesKey("stat_total_moves") to 123,
            booleanPreferencesKey("ach_first_win") to true,
            stringPreferencesKey("theme") to "aurora",
        )

        val new = SaveMigration.migrateToV2(old)

        // Unchanged-position levels stay put.
        assertEquals(true, new[done(1)])
        assertEquals(3, new[stars(1)])
        assertEquals(1, new[best(1)])
        // Shifted levels land on their new ids: 5->6, 6->8, 9->12.
        assertEquals(true, new[done(6)])
        assertEquals(4, new[best(6)])
        assertEquals(true, new[done(8)])
        assertEquals(15, new[best(8)])
        assertNull(new[done(5)], "nothing earned should appear on brand-new level 5")
        // Redesigned old 7 -> new 9: completion and stars carry, best does not.
        assertEquals(true, new[done(9)])
        assertEquals(3, new[stars(9)])
        assertNull(new[best(9)])
        // Snapshot for old 9 moves to new 12 intact.
        assertEquals(
            "a,b,b,a,b;a,b,b,a,b;a,a,f,b,b;b,a,b,b,a;b,a,b,b,a|2",
            new[save(12)],
        )
        assertNull(new[save(9)])
        // Tutorials remap: 1->1, 5->6.
        assertEquals(true, new[tut(1)])
        assertEquals(true, new[tut(6)])
        // Level-independent keys are untouched.
        assertEquals(123, new[intPreferencesKey("stat_total_moves")])
        assertEquals(true, new[booleanPreferencesKey("ach_first_win")])
        assertEquals("aurora", new[stringPreferencesKey("theme")])
        // Versioned.
        assertEquals(SaveMigration.CURRENT_VERSION, new[SaveMigration.SAVE_VERSION])
    }

    @Test
    fun `corrupt and alien entries are dropped without crashing`() {
        val old = preferencesOf(
            // Wrong value type for a level key.
            stringPreferencesKey("best_3") to "not-a-number",
            // Unparseable snapshot.
            save(6) to "garbage-with-no-separator",
            // Snapshot with an invalid board.
            save(8) to "!!;??|3",
            // Level id that never existed in v1.
            done(99) to true,
        )
        val new = SaveMigration.migrateToV2(old)
        assertNull(new[best(3)])
        assertNull(new[save(8)])
        assertNull(new[save(10)])
        assertNull(new[done(99)])
        assertEquals(SaveMigration.CURRENT_VERSION, new[SaveMigration.SAVE_VERSION])
    }

    @Test
    fun `fresh installs just get stamped with the current version`() {
        val new = SaveMigration.migrateToV2(preferencesOf())
        assertEquals(SaveMigration.CURRENT_VERSION, new[SaveMigration.SAVE_VERSION])
        assertEquals(1, new.asMap().size)
    }

    @Test
    fun `migration runs once`() = runBlocking {
        val migration = SaveMigration()
        assertTrue(migration.shouldMigrate(preferencesOf()))
        val migrated = SaveMigration.migrateToV2(preferencesOf(done(1) to true))
        assertFalse(migration.shouldMigrate(migrated))
    }

    @Test
    fun `completion never appears on levels the player did not beat`() {
        val old = preferencesOf(done(3) to true, stars(3) to 2)
        val new = SaveMigration.migrateToV2(old)
        val completedIds = new.asMap().keys
            .filter { it.name.startsWith("done_") }
            .mapNotNull { it.name.substringAfter('_').toIntOrNull() }
        assertEquals(listOf(3), completedIds)
    }
}
