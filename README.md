# LetterLab

A letter-sorting puzzle game for Android. Rearrange living letters — each with
its own personality and ability — until every row and every column of the board
reads in alphabetical order (ascending or descending both count; blanks are
skipped).

## The Alphabet

| Letter | Name | Ability |
|--------|------|---------|
| `a` | Apathetic | Does nothing, but can be moved by others |
| `b` | Basic | Swaps with an adjacent letter |
| `c` | Complex | Swaps with a letter exactly 2 cells away |
| `d` | Delete | Never moves; destroys anything that touches it |
| `e` | Eat | Consumes an adjacent cell and becomes it |
| `f` | Force | Shifts the whole grid one step, wrapping at the edges |
| `g` | Grab | Pulls a letter from its row/column adjacent to itself |
| `h` | Hold | Lifts any letter out of play; swaps what it holds |
| `i` | Immovable | Cannot be moved or touched |
| `j` | Jump | Swaps with a letter exactly 3 cells away |
| `k` | Kick | Boots an adjacent letter to the board edge |
| `l` | Lovely | Kicks all four neighbours to the edges on selection |
| `m` | Monstrous | Drags the four edge letters inward on selection |
| `n` | Nullify | Aura: adjacent letters act like `a`s; adjacent `n`s cancel |
| `o` | Onomatopoeia | Swaps with any vowel anywhere on the board |
| `p` | Purge | Destroys itself and its neighbours on selection |
| `q` | Queen | Swaps any distance along row, column, or diagonal |
| `r` | Remove | Erases every copy of an adjacent letter, becomes an `a` |
| `s` | Spin | Swaps its two opposite neighbours; never moves itself |
| `t` | Toggle | Turns an adjacent letter into the previous letter (a→z) |
| `u` | Upgrade | Turns an adjacent letter into the next letter (z→a) |
| `v` | Virus | Swaps like `b`; whatever it swaps with becomes a `v` |
| `w` | Waltz | Swaps with a diagonally adjacent letter |
| `x` | Xerox | Becomes an exact copy of an adjacent letter |
| `y` | Yeet | Hurls itself to the far edge; passed letters shift back |
| `z` | Zap | Swaps with any cell anywhere on the board |

The full design notes live in [`docs/game_rules.txt`](docs/game_rules.txt),
adapted from the original `LetterLab_FinalProjectFall2024` design document.

## Features

- 80 handcrafted levels across 10 worlds, defined in
  [`app/src/main/assets/levels.json`](app/src/main/assets/levels.json) —
  add new levels and worlds without touching code
- Every level is machine-verified solvable; pars are calibrated to
  solver-optimal (or best-known) move counts, so every star is earnable
- Each world introduces a small family of letters (tutorial level per
  letter), then combines them; later worlds remix the whole alphabet
- Star ratings (par-based), best-move tracking, and sequential unlocks
- An in-game hint system backed by the same solver (escalating weighted
  A* + beam search), with solved-path caching for instant follow-ups
- Real tile movement: the engine emits a deterministic trace of every
  letter's journey per move, and the UI animates letters sliding (and
  perishing) along it — undo slides everything back
- An in-game dictionary with animated before/after examples, strategy
  tips, and special-interaction notes for all 26 letters
- Unlimited undo, level restart, and mid-level resume
- Six procedurally animated, fully offline background themes
  (unlocked by clearing worlds)
- Synthesized sound effects and a synthesized ambient music loop
  (zero audio assets), haptics, and lifetime statistics
- Achievements for every world, star milestones, and play habits
- Settings for sound, music, haptics, theme, and progress reset

## Architecture

Kotlin, single activity, Jetpack Compose, MVVM.

```
domain/        Pure-Kotlin rules engine (Board, GameEngine) — no Android deps
domain/solver/ BFS + weighted-A* + beam-search solver (hints, verification)
data/          Level catalog (JSON asset) + DataStore repositories
achievements/  Achievement definitions; derived purely from persisted state
audio/         Synthesized SoundPool effects, ambient music loop, haptics
ui/            Compose screens, ViewModels, animated backgrounds, navigation
```

## Save migration

Saves are versioned (`save_version` in the preferences DataStore) and
migrated by `data/SaveMigration.kt`, which DataStore runs before the
first read. The v1→v2 migration remaps the original 25-level campaign's
completion, stars, best moves, resume snapshots, and seen-tutorial flags
onto the new 80-level ids. It is conservative: best moves and snapshots
only carry where the grid is byte-identical (`SaveMigrationTest` proves
the mapping against the v1 grids verbatim); corrupt or unmappable entries
are dropped, never fatal; nothing is marked complete that wasn't earned.

## Testing & level authoring

The engine is fully unit-tested (`app/src/test`): every letter mechanic is
verified against the worked examples in the design document, and the
shipped catalog is gated by three test layers:

- `LevelCatalogTest` — structural: parseable, rectangular, unique ids,
  not pre-solved, pars positive, tutorial letters present.
- `CampaignSolvabilityTest` — the release gate: every level has a
  solver-found, engine-replayable solution **within par**, so all
  240 stars are earnable.
- `CampaignIntegrityTest` — progression: no tutorial has a within-par
  solution that skips its featured letter (budget-bounded exhaustive
  search; passive letters d/i/n exempt), and every achievement and
  background theme is reachable.

The authoring workflow: draft candidate grids in `LevelSandboxTest`
(prints optimal solutions as `letter@row,col -> row,col` chains), then run
`CampaignCalibrationTest` to regenerate `build/calibration.txt` with
best-known solution lengths and par verdicts for the whole campaign.
Authoring gotchas the tests exist to catch: 2×2 boards are always
solved, blanks are skipped when sorting (they make high letters like
n/s/w placeable), and any letter other than a/d/i/n is an actor that can
create bypass solutions.

## Building

Requires JDK 17 and the Android SDK (compileSdk 35).

```
./gradlew :app:assembleDebug        # build
./gradlew :app:testDebugUnitTest    # engine + level catalog tests
```
