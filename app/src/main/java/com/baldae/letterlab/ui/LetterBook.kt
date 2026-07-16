package com.baldae.letterlab.ui

/** Player-facing copy for each letter, shared by the help screen and the in-game hint bar. */
data class LetterInfo(
    val letter: Char,
    val name: String,
    val blurb: String,
    /** Optional before → after example, straight from the design doc. */
    val example: Pair<String, String>? = null,
)

object LetterBook {

    val entries: List<LetterInfo> = listOf(
        LetterInfo(
            'a', "Apathetic",
            "Does nothing on its own — but other letters can move it around.",
        ),
        LetterInfo(
            'b', "Basic",
            "Swaps places with a letter directly next to it.",
            "aba" to "baa",
        ),
        LetterInfo(
            'c', "Complex",
            "Swaps with a letter exactly two cells away in its row or column.",
            "aacaa" to "caaaa",
        ),
        LetterInfo(
            'd', "Delete",
            "Never moves on its own and destroys any letter that touches it. Grabbing or kicking one wipes everything in its path.",
            "adb" to "ad_",
        ),
        LetterInfo(
            'e', "Eat",
            "Devours an adjacent cell and becomes whatever it ate. The only letter that can safely swallow a d.",
            "abcec" to "ab_cc",
        ),
        LetterInfo(
            'f', "Force",
            "Shoves the entire grid one step in a direction. Letters wrap around the edges; d and i stand firm.",
            "fab" to "bfa",
        ),
        LetterInfo(
            'g', "Grab",
            "Reaches along its row and column, pulling a letter right next to itself while the rest slide back.",
            "abcag" to "bcaag",
        ),
        LetterInfo(
            'h', "Hold",
            "Lifts any letter on the board above its head and out of play; tap again to swap what it holds.",
        ),
        LetterInfo(
            'i', "Immovable",
            "Cannot be moved or touched by anything. Sliding letters pass straight over it.",
        ),
        LetterInfo(
            'j', "Jump",
            "Swaps with a letter exactly three cells away — the long-range cousin of b and c.",
            "abcjcba" to "jbcacba",
        ),
        LetterInfo(
            'k', "Kick",
            "Boots an adjacent letter to the edge of the board; everything in between shuffles closer.",
            "bkabba" to "bkbbaa",
        ),
        LetterInfo(
            'l', "Lovely",
            "Fires the moment you tap it: kicks all four of its neighbours to the edges.",
            "bacblccbb" to "bbaclcbbc",
        ),
        LetterInfo(
            'm', "Monstrous",
            "Fires the moment you tap it: drags the four edge letters of its row and column inward.",
            "bacbmccbb" to "acbbmbccb",
        ),
    )

    fun of(letter: Char): LetterInfo? = entries.find { it.letter == letter }
}
