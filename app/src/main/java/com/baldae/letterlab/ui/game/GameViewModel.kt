package com.baldae.letterlab.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baldae.letterlab.AppContainer
import com.baldae.letterlab.achievements.Achievement
import com.baldae.letterlab.achievements.AchievementBook
import com.baldae.letterlab.audio.GameSound
import com.baldae.letterlab.data.LevelDef
import com.baldae.letterlab.data.TutorialDef
import com.baldae.letterlab.domain.Board
import com.baldae.letterlab.domain.GameEngine
import com.baldae.letterlab.domain.MoveResult
import com.baldae.letterlab.domain.Pos
import com.baldae.letterlab.domain.Selection
import com.baldae.letterlab.domain.TileMove
import com.baldae.letterlab.domain.solver.Move
import com.baldae.letterlab.domain.solver.Solver
import com.baldae.letterlab.ui.background.BackgroundTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val level: LevelDef,
    val worldName: String,
    val board: Board,
    val selected: Pos? = null,
    val targets: Set<Pos> = emptySet(),
    val moves: Int = 0,
    val bestMoves: Int? = null,
    val canUndo: Boolean = false,
    val won: Boolean = false,
    val earnedStars: Int = 0,
    val isNewBest: Boolean = false,
    val hasNextLevel: Boolean = false,
    val tutorial: TutorialDef? = null,
    /** Bumps on every board change so the UI can key one-shot animations. */
    val moveStamp: Int = 0,
    /** The physical letter journeys of the latest board change, for animation. */
    val lastMoves: List<TileMove> = emptyList(),
    /** Bumps on every rejected tap so the UI can shake. */
    val invalidStamp: Int = 0,
    /** Achievement toasts waiting to be shown. */
    val pendingAchievements: List<Achievement> = emptyList(),
    /** Background theme newly unlocked by this win, if any. */
    val unlockedThemeName: String? = null,
    val hint: Move? = null,
    val hintState: HintState = HintState.IDLE,
    val loading: Boolean = true,
)

enum class HintState { IDLE, WORKING, UNAVAILABLE }

class GameViewModel(
    private val container: AppContainer,
    private val levelId: Int,
) : ViewModel() {

    private val catalog = container.levelRepository.catalog
    private val level: LevelDef = requireNotNull(catalog.level(levelId)) {
        "Unknown level id $levelId"
    }

    /** Boards to undo back to, with the trace of the move that left them. */
    private data class HistoryEntry(val board: Board, val moves: Int, val trace: List<TileMove>)

    private val history = ArrayDeque<HistoryEntry>()

    private val _state = MutableStateFlow(
        GameUiState(
            level = level,
            worldName = catalog.worldOf(levelId)?.name.orEmpty(),
            board = level.board(),
            hasNextLevel = catalog.nextLevel(levelId) != null,
        )
    )
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    val nextLevelId: Int? get() = catalog.nextLevel(levelId)?.id

    init {
        viewModelScope.launch {
            val saved = container.progressRepository.loadInProgress(levelId)
            val seenTutorials = container.progressRepository.seenTutorials.first()
            val progress = container.progressRepository.progress.first()[levelId]
            _state.update {
                it.copy(
                    board = saved?.board ?: it.board,
                    moves = saved?.moves ?: 0,
                    bestMoves = progress?.bestMoves,
                    tutorial = level.tutorial?.takeIf { _ -> levelId !in seenTutorials },
                    loading = false,
                )
            }
        }
    }

    fun onCellTap(pos: Pos) {
        val s = _state.value
        if (s.won || s.loading || s.tutorial != null) return

        val selected = s.selected
        if (selected == null) {
            trySelect(pos)
            return
        }

        when {
            pos == selected -> deselect()
            pos in s.targets -> applyMove(selected, pos)
            // Quality of life: tapping another targetable letter switches
            // the selection instead of forcing a deselect-tap first.
            // Immediate letters (l, m) are excluded so a stray tap can't fire them.
            GameEngine.selectionFor(s.board, pos) is Selection.NeedsTarget -> trySelect(pos)
            else -> deselect()
        }
    }

    private fun trySelect(pos: Pos) {
        val s = _state.value
        when (val selection = GameEngine.selectionFor(s.board, pos)) {
            is Selection.NeedsTarget -> {
                if (selection.targets.isEmpty()) {
                    rejectTap()
                    return
                }
                container.soundManager.play(GameSound.SELECT)
                container.haptics.tick()
                _state.update { it.copy(selected = pos, targets = selection.targets) }
            }
            is Selection.Immediate -> {
                val result = GameEngine.applyWithTrace(s.board, pos, null) ?: return rejectTap()
                container.haptics.tick()
                commitMove(result)
            }
            Selection.NotSelectable ->
                if (!s.board[pos].isEmpty) rejectTap()
        }
    }

    private fun deselect() {
        // Unlike the original, cancelling a selection is free.
        _state.update { it.copy(selected = null, targets = emptySet()) }
    }

    private fun applyMove(origin: Pos, target: Pos) {
        val s = _state.value
        val result = GameEngine.applyWithTrace(s.board, origin, target) ?: return rejectTap()
        commitMove(result)
    }

    private fun commitMove(result: MoveResult) {
        val s = _state.value
        history.addLast(HistoryEntry(s.board, s.moves, result.tileMoves))
        val newBoard = result.board
        val moves = s.moves + 1
        val won = GameEngine.isSolved(newBoard)

        _state.update {
            it.copy(
                board = newBoard,
                moves = moves,
                selected = null,
                targets = emptySet(),
                canUndo = true,
                moveStamp = it.moveStamp + 1,
                lastMoves = result.tileMoves,
                hint = null,
                hintState = HintState.IDLE,
            )
        }

        viewModelScope.launch { container.statsRepository.addMoves(1) }

        if (won) {
            onWin(moves)
        } else {
            container.soundManager.play(GameSound.MOVE)
            container.haptics.move()
            viewModelScope.launch {
                container.progressRepository.saveInProgress(levelId, newBoard, moves)
            }
        }
    }

    private fun onWin(moves: Int) {
        val stars = level.starsFor(moves)
        val previousBest = _state.value.bestMoves
        val isNewBest = previousBest == null || moves < previousBest

        container.soundManager.play(GameSound.WIN)
        container.haptics.win()

        _state.update {
            it.copy(
                won = true,
                earnedStars = stars,
                isNewBest = isNewBest,
                bestMoves = if (isNewBest) moves else previousBest,
            )
        }

        viewModelScope.launch {
            val progressBefore = container.progressRepository.progress.first()
            container.progressRepository.recordWin(levelId, moves, stars)
            container.statsRepository.addWin()
            val progressAfter = container.progressRepository.progress.first()
            val unlockedTheme = BackgroundTheme.entries.firstOrNull { theme ->
                !BackgroundTheme.isUnlocked(theme, catalog, progressBefore) &&
                    BackgroundTheme.isUnlocked(theme, catalog, progressAfter)
            }
            if (unlockedTheme != null) {
                _state.update { it.copy(unlockedThemeName = unlockedTheme.displayName) }
            }
            unlockDeservedAchievements()
        }
    }

    /**
     * Computes a suggested move on a background thread. Budgets are sized so
     * even 7x7 boards answer within a few seconds on a phone.
     */
    fun requestHint() {
        val s = _state.value
        if (s.won || s.loading || s.tutorial != null || s.hintState == HintState.WORKING) return
        _state.update { it.copy(hintState = HintState.WORKING, hint = null) }
        val board = s.board
        viewModelScope.launch(Dispatchers.Default) {
            val solver = Solver(bfsNodeBudget = 120_000, beamWidth = 250, maxBeamMoves = 40)
            val move = solver.hint(board)
            _state.update {
                // The board may have changed while we were thinking; discard if so.
                if (it.board != board) it.copy(hintState = HintState.IDLE)
                else it.copy(
                    hint = move,
                    hintState = if (move == null) HintState.UNAVAILABLE else HintState.IDLE,
                )
            }
        }
    }

    fun undo() {
        val s = _state.value
        if (s.won || history.isEmpty()) return
        val (board, moves, trace) = history.removeLast()
        container.soundManager.play(GameSound.UNDO)
        container.haptics.tick()
        // Surviving letters slide back along their reversed journeys;
        // letters the move destroyed simply reappear.
        val reversed = trace.filter { !it.vanishes }.map { TileMove(it.to, it.from, it.cell) }
        _state.update {
            it.copy(
                board = board,
                moves = moves,
                selected = null,
                targets = emptySet(),
                canUndo = history.isNotEmpty(),
                moveStamp = it.moveStamp + 1,
                lastMoves = reversed,
            )
        }
        viewModelScope.launch {
            container.statsRepository.addUndo()
            if (moves == 0) {
                container.progressRepository.clearInProgress(levelId)
            } else {
                container.progressRepository.saveInProgress(levelId, board, moves)
            }
            unlockDeservedAchievements()
        }
    }

    fun restart() {
        history.clear()
        _state.update {
            it.copy(
                board = level.board(),
                moves = 0,
                selected = null,
                targets = emptySet(),
                canUndo = false,
                won = false,
                earnedStars = 0,
                isNewBest = false,
                moveStamp = it.moveStamp + 1,
                lastMoves = emptyList(),
                hint = null,
                hintState = HintState.IDLE,
            )
        }
        viewModelScope.launch { container.progressRepository.clearInProgress(levelId) }
    }

    fun dismissTutorial() {
        _state.update { it.copy(tutorial = null) }
        viewModelScope.launch { container.progressRepository.markTutorialSeen(levelId) }
    }

    fun showTutorial() {
        level.tutorial?.let { tut -> _state.update { it.copy(tutorial = tut) } }
    }

    fun consumeAchievement() {
        _state.update { it.copy(pendingAchievements = it.pendingAchievements.drop(1)) }
    }

    private fun rejectTap() {
        container.soundManager.play(GameSound.INVALID, volume = 0.6f)
        container.haptics.invalid()
        _state.update { it.copy(invalidStamp = it.invalidStamp + 1) }
    }

    private suspend fun unlockDeservedAchievements() {
        val progress = container.progressRepository.progress.first()
        val stats = container.statsRepository.stats.first()
        val deserved = AchievementBook.deservedIds(catalog, progress, stats)
        val newlyUnlocked = container.achievementsRepository.unlock(deserved)
        if (newlyUnlocked.isNotEmpty()) {
            container.soundManager.play(GameSound.ACHIEVEMENT)
            val achievements = newlyUnlocked.mapNotNull(AchievementBook::byId)
            _state.update { it.copy(pendingAchievements = it.pendingAchievements + achievements) }
        }
    }

    companion object {
        fun factory(container: AppContainer, levelId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GameViewModel(container, levelId) as T
            }
    }
}
