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

The full design notes live in [`docs/game_rules.txt`](docs/game_rules.txt).

## Features

- 25 handcrafted levels across 5 worlds, defined in
  [`app/src/main/assets/levels.json`](app/src/main/assets/levels.json) —
  add new levels and worlds without touching code
- Every level is machine-verified solvable; pars are calibrated from
  solver-optimal (or best-known) move counts
- Star ratings (par-based), best-move tracking, and sequential unlocks
- An in-game hint system backed by the same solver (BFS + beam search)
- Real tile movement: the engine emits a deterministic trace of every
  letter's journey per move, and the UI animates letters sliding (and
  perishing) along it — undo slides everything back
- Unlimited undo, level restart, and mid-level resume
- Six procedurally animated, fully offline background themes
  (unlocked by clearing worlds)
- Synthesized sound effects and a synthesized ambient music loop
  (zero audio assets), haptics, tutorials for every new letter,
  achievements, and lifetime statistics
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

The engine is fully unit-tested (`app/src/test`): every letter mechanic is
verified against the worked examples in the design document, the shipped
level catalog is validated (parseable, rectangular, not pre-solved, par
sanity), and a solver-backed test proves each campaign level solvable.
`CampaignReportTest` regenerates `build/campaign-report.txt` with solution
lengths for par calibration.

## Building

Requires JDK 17 and the Android SDK (compileSdk 35).

```
./gradlew :app:assembleDebug        # build
./gradlew :app:testDebugUnitTest    # engine + level catalog tests
```
