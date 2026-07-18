package com.baldae.letterlab.ui

/** Player-facing copy for each letter, shared by the help screen and the in-game hint bar. */
data class LetterInfo(
    val letter: Char,
    val name: String,
    val blurb: String,
    /**
     * Optional before → after example, straight from the design doc.
     * Multi-row boards use '\n' between rows; both sides share one shape.
     */
    val example: Pair<String, String>? = null,
    /** One practical piece of strategy advice. */
    val tip: String? = null,
    /** Notable interactions with other letters. */
    val interactions: String? = null,
)

object LetterBook {

    val entries: List<LetterInfo> = listOf(
        LetterInfo(
            'a', "Apathetic",
            "Does nothing on its own — but other letters can move it around.",
            tip = "Every a is a free space: park letters past it, kick it into gaps, feed it to an e.",
        ),
        LetterInfo(
            'b', "Basic",
            "Swaps places with a letter directly next to it.",
            "aba" to "baa",
            tip = "The workhorse. When a puzzle stalls, look for the b that repositions everything else.",
            interactions = "Swapping into a d destroys the b.",
        ),
        LetterInfo(
            'c', "Complex",
            "Swaps with a letter exactly two cells away in its row or column.",
            "aacaa" to "caaaa",
            tip = "c can't touch its neighbours at all — use that blind spot to slip past letters b can't avoid.",
        ),
        LetterInfo(
            'd', "Delete",
            "Never moves on its own and destroys any letter that touches it. Grabbing or kicking one wipes everything in its path.",
            "adb" to "ad_",
            tip = "d is a wall, a trash bin, and a trap. Decide early which one this puzzle needs it to be.",
            interactions = "e eats it. p bombs it. r purges every copy. v infects it. n switches it off entirely.",
        ),
        LetterInfo(
            'e', "Eat",
            "Devours an adjacent cell and becomes whatever it ate. The only letter that can safely swallow a d.",
            "abcec" to "ab_cc",
            tip = "e is a shapeshifter: eat the letter you're missing, not the one that's in the way.",
            interactions = "Eating a blank destroys the e. Eating a d makes it the new wall.",
        ),
        LetterInfo(
            'f', "Force",
            "Shoves the entire grid one step in a direction. Letters wrap around the edges; d and i stand firm.",
            "fab" to "bfa",
            tip = "Count where every letter lands before you push — one f move rearranges the whole board.",
            interactions = "Anything shoved into a d is destroyed. i never budges.",
        ),
        LetterInfo(
            'g', "Grab",
            "Reaches along its row and column, pulling a letter right next to itself while the rest slide back.",
            "abcag" to "bcaag",
            tip = "g compresses a line toward itself — great for closing gaps that blanks left behind.",
            interactions = "Grabbing a d drags it the whole way, deleting everything in between.",
        ),
        LetterInfo(
            'h', "Hold",
            "Lifts any letter on the board above its head and out of play; tap again to swap what it holds.",
            tip = "h is a pocket: stash the letter that's blocking a row, fix the row, then put it back somewhere useful.",
            interactions = "Holding a d destroys the h and everything it carried. It can never hold another h.",
        ),
        LetterInfo(
            'i', "Immovable",
            "Cannot be moved or touched by anything. Sliding letters pass straight over it.",
            tip = "Solve around the i first — every row and column it sits in is already partly decided.",
            interactions = "Only an n's aura switches it off; then it moves like an a.",
        ),
        LetterInfo(
            'j', "Jump",
            "Swaps with a letter exactly three cells away — the long-range cousin of b and c.",
            "abcjcba" to "jbcacba",
            tip = "On small boards j can look useless — check both its row and its column before writing it off.",
        ),
        LetterInfo(
            'k', "Kick",
            "Boots an adjacent letter to the edge of the board; everything in between shuffles closer.",
            "bkabba" to "bkbbaa",
            tip = "Kick sorts from the outside in: send the biggest or smallest letters to the walls first.",
            interactions = "Kicking a d wipes its whole flight path.",
        ),
        LetterInfo(
            'l', "Lovely",
            "Fires the moment you tap it: kicks all four of its neighbours to the edges.",
            "bacblccbb" to "bbaclcbbc",
            tip = "One tap moves up to four letters — line up its neighbours before you pull the trigger.",
        ),
        LetterInfo(
            'm', "Monstrous",
            "Fires the moment you tap it: drags the four edge letters of its row and column inward.",
            "bacbmccbb" to "acbbmbccb",
            tip = "m cares about the edges, not its neighbours. Stage the right letters at the walls first.",
        ),
        LetterInfo(
            'n', "Nullify",
            "A passive aura: letters directly next to an n act like a's — they can't act, can't delete, can't protect themselves. Two adjacent n's switch each other off.",
            "bdn" to "dbn",
            tip = "Park the n beside the letter you want disarmed. A nullified d is just a movable tile.",
            interactions = "The only counter to i. Nullified letters still sort by their real name.",
        ),
        LetterInfo(
            'o', "Onomatopoeia",
            "Swaps places with any vowel on the board — a, e, u, or another o — no matter how far away.",
            "abaobba" to "obaabba",
            tip = "Boards are full of a's, so o is almost always one tap from anywhere. Plan its landing, not its launch.",
            interactions = "i is not on its guest list. Nothing is.",
        ),
        LetterInfo(
            'p', "Purge",
            "Fires the moment you tap it: destroys itself and all four of its neighbours.",
            "apd" to "___",
            tip = "p trades five cells for one tap. Make sure at least one of them deserved it.",
            interactions = "The only letter that can destroy a d. Only an i survives the blast.",
        ),
        LetterInfo(
            'q', "Queen",
            "Swaps with any letter any distance along her row, column, or diagonals — nothing in between blocks her.",
            "qaaab" to "baaaq",
            tip = "Eight directions, unlimited range: q usually has more moves than every other letter combined. Look twice.",
            interactions = "Touching a d still ends her reign.",
        ),
        LetterInfo(
            'r', "Remove",
            "Erases every copy of an adjacent letter from the board, then settles down as an a.",
            "accdrca" to "a__da_a",
            tip = "r is board-wide: clearing one crowded letter can fix three rows at once — or empty them.",
            interactions = "Removing d's costs the r its life. It can never remove an i.",
        ),
        LetterInfo(
            's', "Spin",
            "Swaps the two letters on opposite sides of itself; the s never moves.",
            "bsa" to "asb",
            tip = "Think of s as a revolving door — it can fix two lines at once without leaving its post.",
            interactions = "If a d sits on one side, the letter opposite it is destroyed instead of swapped.",
        ),
        LetterInfo(
            't', "Toggle",
            "Rewrites an adjacent letter into the previous letter of the alphabet: c→b, b→a — and a wraps to z.",
            "btc" to "atc",
            tip = "t makes the letters you're missing. Wrapping an a to z turns filler into the biggest letter in the game.",
            interactions = "Toggling an h drops what it held. Reaching into a d is fatal.",
        ),
        LetterInfo(
            'u', "Upgrade",
            "Rewrites an adjacent letter into the next letter of the alphabet: a→b, b→c — and z wraps to a.",
            "buc" to "cuc",
            tip = "u turns spare a's into whatever the row needs. The exact mirror of t.",
            interactions = "Upgrading an h drops what it held. Reaching into a d is fatal.",
        ),
        LetterInfo(
            'v', "Virus",
            "Swaps like a b — but whatever it swaps with becomes a v too.",
            "abvac" to "avvac",
            tip = "Every v move makes another v. Sometimes that's a plague; sometimes it's exactly the letter you need twice.",
            interactions = "A d destroys the v — and catches the virus. i is immune.",
        ),
        LetterInfo(
            'w', "Waltz",
            "Glides diagonally: swaps with any letter one step diagonal from it. The only letter that moves this way.",
            "wa\nab" to "ba\naw",
            tip = "w changes its row and its column in one move — the step every cardinal letter wishes it had.",
            interactions = "A d on the diagonal is as fatal as any other.",
        ),
        LetterInfo(
            'x', "Xerox",
            "Becomes an exact copy of an adjacent letter; the original stays put.",
            "caxb" to "cabb",
            tip = "Need a second e, k, or even d? Photocopy one. x copies an h's held letter too.",
            interactions = "The only way to create a d. It cannot copy an i or a blank.",
        ),
        LetterInfo(
            'y', "Yeet",
            "Hurls itself to the far edge of the board; every letter it sails past shifts one step back.",
            "yaab" to "aaby",
            tip = "y is a kick you aim at yourself — clear out of a crowded middle and tidy the line behind you.",
            interactions = "Sails over i. Landing on a d is a very short flight.",
        ),
        LetterInfo(
            'z', "Zap",
            "The final letter: swaps with any cell anywhere on the board. No lines, no limits.",
            "zaa\naab" to "baa\naaz",
            tip = "z can fix any single cell in one tap — so spend it on the cell nothing else can reach.",
            interactions = "Only i is beyond it, and a d still ends it.",
        ),
    )

    fun of(letter: Char): LetterInfo? = entries.find { it.letter == letter }
}
