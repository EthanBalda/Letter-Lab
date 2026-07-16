package com.baldae.letterlab.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baldae.letterlab.domain.Board
import com.baldae.letterlab.domain.Cell
import com.baldae.letterlab.domain.Pos
import com.baldae.letterlab.domain.TileMove
import com.baldae.letterlab.domain.solver.Move
import com.baldae.letterlab.ui.LetterBook
import com.baldae.letterlab.ui.components.LetterTile
import com.baldae.letterlab.ui.components.TileState
import com.baldae.letterlab.ui.components.rememberTargetPulse
import com.baldae.letterlab.ui.theme.LabAmber
import com.baldae.letterlab.ui.theme.LabTeal
import com.baldae.letterlab.ui.theme.letterColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import com.baldae.letterlab.ui.theme.TextDim
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onNextLevel: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showInfo by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp),
        ) {
            GameTopBar(
                state = state,
                onBack = onBack,
                onUndo = viewModel::undo,
                onRestart = viewModel::restart,
                onInfo = { showInfo = true },
                onHint = viewModel::requestHint,
            )

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ShakingBoard(
                    board = state.board,
                    selected = state.selected,
                    targets = state.targets,
                    hint = state.hint,
                    invalidStamp = state.invalidStamp,
                    entranceKey = state.level.id,
                    lastMoves = state.lastMoves,
                    moveStamp = state.moveStamp,
                    onCellTap = viewModel::onCellTap,
                )
            }

            HintBar(state)
            Spacer(Modifier.height(12.dp))
        }

        // First-visit tutorial or on-demand rules.
        state.tutorial?.let { tut ->
            InfoOverlay(
                title = tut.title,
                body = tut.body,
                letter = tut.letter?.singleOrNull(),
                onDismiss = viewModel::dismissTutorial,
            )
        }
        if (showInfo && state.tutorial == null) {
            InfoOverlay(
                title = state.level.name,
                body = state.level.tutorial?.body
                    ?: ("Make every row and every column read in alphabetical order — " +
                        "ascending or descending — and the level is solved. Blanks don't count."),
                letter = state.level.tutorial?.letter?.singleOrNull(),
                onDismiss = { showInfo = false },
            )
        }

        if (state.won) {
            WinOverlay(
                state = state,
                onNext = { viewModel.nextLevelId?.let(onNextLevel) },
                onReplay = viewModel::restart,
                onLevels = onBack,
            )
        }

        AchievementToast(
            state = state,
            onConsumed = viewModel::consumeAchievement,
            modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding(),
        )
    }
}

// ------------------------------------------------------------------ top bar

@Composable
private fun GameTopBar(
    state: GameUiState,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onInfo: () -> Unit,
    onHint: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${state.level.id}. ${state.level.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    state.worldName,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextDim,
                )
            }
            IconButton(
                onClick = onUndo,
                enabled = state.canUndo && !state.won,
                modifier = Modifier.semantics { contentDescription = "Undo move" },
            ) {
                Text(
                    "↶",
                    fontSize = 24.sp,
                    color = if (state.canUndo && !state.won)
                        MaterialTheme.colorScheme.onSurface
                    else TextDim.copy(alpha = 0.4f),
                )
            }
            IconButton(onClick = onRestart) {
                Icon(Icons.Default.Refresh, contentDescription = "Restart level")
            }
            IconButton(onClick = onInfo) {
                Icon(Icons.Default.Info, contentDescription = "Level info")
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatChip("Moves", state.moves.toString())
            StatChip("Par", state.level.par.toString())
            StatChip("Best", state.bestMoves?.toString() ?: "—")
            Spacer(Modifier.weight(1f))
            HintButton(state.hintState, enabled = !state.won, onHint = onHint)
        }
    }
}

@Composable
private fun HintButton(hintState: HintState, enabled: Boolean, onHint: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        modifier = Modifier
            .semantics { contentDescription = "Show a hint" }
            .clickable(enabled = enabled && hintState != HintState.WORKING, onClick = onHint),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hintState == HintState.WORKING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = LabTeal,
                )
                Spacer(Modifier.size(6.dp))
            }
            Text(
                "Hint",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = LabTeal,
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextDim)
            Spacer(Modifier.size(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// -------------------------------------------------------------------- board

@Composable
private fun ShakingBoard(
    board: Board,
    selected: Pos?,
    targets: Set<Pos>,
    hint: Move?,
    invalidStamp: Int,
    entranceKey: Int,
    lastMoves: List<TileMove>,
    moveStamp: Int,
    onCellTap: (Pos) -> Unit,
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(invalidStamp) {
        if (invalidStamp > 0) {
            shake.animateTo(
                0f,
                keyframes {
                    durationMillis = 280
                    (-10f) at 40
                    10f at 100
                    (-6f) at 160
                    4f at 220
                    0f at 280
                },
            )
        }
    }
    Box(Modifier.offset { IntOffset(shake.value.roundToInt(), 0) }) {
        BoardGrid(board, selected, targets, hint, entranceKey, lastMoves, moveStamp, onCellTap)
    }
}

/** How long a one-cell slide takes; long journeys ease over the same beat. */
private const val SLIDE_MILLIS = 220

@Composable
private fun BoardGrid(
    board: Board,
    selected: Pos?,
    targets: Set<Pos>,
    hint: Move?,
    entranceKey: Int,
    lastMoves: List<TileMove>,
    moveStamp: Int,
    onCellTap: (Pos) -> Unit,
) {
    // One shared pulse for every target tile on the board.
    val pulse = rememberTargetPulse()

    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val spacing = 6.dp
        val tileFromWidth = (maxWidth - spacing * (board.cols - 1)) / board.cols
        val tileFromHeight = (maxHeight - spacing * (board.rows - 1)) / board.rows
        val tile = minOf(tileFromWidth, tileFromHeight, 64.dp)
        val stepPx = with(LocalDensity.current) { (tile + spacing).toPx() }

        // Shared progress for the latest move's slide animation. Board state
        // is already final — this is purely cosmetic, so input is never gated.
        val slide = remember { Animatable(1f) }
        LaunchedEffect(moveStamp) {
            if (moveStamp == 0 || lastMoves.isEmpty()) {
                slide.snapTo(1f)
            } else {
                slide.snapTo(0f)
                slide.animateTo(1f, tween(SLIDE_MILLIS, easing = FastOutSlowInEasing))
            }
        }
        // Destinations whose real tile stays invisible until its ghost lands.
        val slideDestinations = remember(moveStamp) {
            lastMoves.filter { !it.vanishes }.map { it.to }.toSet()
        }
        val tracedPositions = remember(moveStamp) {
            lastMoves.flatMap { listOf(it.from, it.to) }.toSet()
        }

        Box {
            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                for (r in 0 until board.rows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        for (c in 0 until board.cols) {
                            val pos = Pos(r, c)
                            val tileState = when {
                                pos == selected -> TileState.SELECTED
                                hint != null && (pos == hint.origin || pos == hint.target) ->
                                    TileState.HINT
                                pos in targets -> TileState.TARGET
                                selected != null -> TileState.DIMMED
                                else -> TileState.NORMAL
                            }
                            // key() keeps each tile's remember{} slots stable and
                            // restarts the entrance stagger when the level changes.
                            androidx.compose.runtime.key(entranceKey, pos) {
                                LetterTile(
                                    cell = board[pos],
                                    size = tile,
                                    state = tileState,
                                    pulse = pulse,
                                    appearDelayMillis = (r + c) * 40,
                                    suppressPop = pos in tracedPositions,
                                    onClick = { onCellTap(pos) },
                                    modifier = Modifier.graphicsLayer {
                                        // Hidden while its ghost is in flight;
                                        // draw-phase read, no recomposition.
                                        alpha = if (slide.value < 1f && pos in slideDestinations) 0f else 1f
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Ghost layer: the travelling letters, rendered above the grid so
            // they glide over static tiles regardless of row order.
            androidx.compose.runtime.key(moveStamp) {
                for (move in lastMoves) {
                    LetterTile(
                        cell = move.cell,
                        size = tile,
                        modifier = Modifier
                            .offset {
                                val p = slide.value
                                IntOffset(
                                    (((move.from.col + (move.to.col - move.from.col) * p)) * stepPx).roundToInt(),
                                    (((move.from.row + (move.to.row - move.from.row) * p)) * stepPx).roundToInt(),
                                )
                            }
                            .graphicsLayer {
                                alpha = when {
                                    slide.value >= 1f -> 0f
                                    move.vanishes -> 1f - slide.value
                                    else -> 1f
                                }
                            },
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------- hint bar

@Composable
private fun HintBar(state: GameUiState) {
    val selectedLetter = state.selected?.let { state.board[it].letter }
    val info = selectedLetter?.let(LetterBook::of)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier.heightIn(min = 44.dp).padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when {
                    info != null -> "${info.letter} • ${info.name} — ${info.blurb}"
                    state.hint != null -> "Try the highlighted move."
                    state.hintState == HintState.UNAVAILABLE ->
                        "No hint found from here — try undoing a move or two."
                    else -> "Sort every row and column alphabetically — either direction works."
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    info != null -> MaterialTheme.colorScheme.onSurface
                    state.hint != null -> LabAmber
                    else -> TextDim
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ----------------------------------------------------------------- overlays

@Composable
private fun InfoOverlay(
    title: String,
    body: String,
    letter: Char?,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.padding(28.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (letter != null) {
                    LetterTile(cell = Cell(letter), size = 56.dp)
                    Spacer(Modifier.height(14.dp))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                Button(onClick = onDismiss) { Text("Got it") }
            }
        }
    }
}

@Composable
private fun WinOverlay(
    state: GameUiState,
    onNext: () -> Unit,
    onReplay: () -> Unit,
    onLevels: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center,
    ) {
        ConfettiBurst()
        Card(
            modifier = Modifier.padding(28.dp).fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Level Solved!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(14.dp))
                StarRow(earned = state.earnedStars)
                Spacer(Modifier.height(14.dp))
                Text(
                    buildString {
                        append(if (state.moves == 1) "1 move" else "${state.moves} moves")
                        append(" • par ${state.level.par}")
                        if (state.isNewBest) append("  •  New best!")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isNewBest) LabAmber else TextDim,
                )
                state.unlockedThemeName?.let { themeName ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Theme unlocked: $themeName ✨",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = LabTeal,
                    )
                }
                Spacer(Modifier.height(22.dp))
                if (state.hasNextLevel) {
                    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                        Text("Next Level")
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onReplay, modifier = Modifier.weight(1f)) {
                        Text("Replay")
                    }
                    OutlinedButton(onClick = onLevels, modifier = Modifier.weight(1f)) {
                        Text("Levels")
                    }
                }
            }
        }
    }
}

/** A single celebratory burst of letter-colored confetti behind the win card. */
@Composable
private fun ConfettiBurst(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(2400, easing = LinearOutSlowInEasing))
    }
    val particles = remember {
        val random = kotlin.random.Random(42)
        val palette = "bcdefghjklm".map(::letterColor)
        List(44) { index ->
            ConfettiParticle(
                angle = random.nextFloat() * 2f * PI.toFloat(),
                speed = 0.30f + random.nextFloat() * 0.70f,
                size = 6f + random.nextFloat() * 8f,
                spin = random.nextFloat() * 720f - 360f,
                color = palette[index % palette.size],
            )
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val p = progress.value
        if (p <= 0f || p >= 1f) return@Canvas
        val centerX = size.width / 2f
        val centerY = size.height * 0.38f
        for (particle in particles) {
            val distance = particle.speed * size.width * 0.55f * p
            val x = centerX + cos(particle.angle) * distance
            val y = centerY + sin(particle.angle) * distance + size.height * 0.25f * p * p
            rotate(particle.spin * p, pivot = Offset(x, y)) {
                drawRect(
                    color = particle.color.copy(alpha = 1f - p),
                    topLeft = Offset(x, y),
                    size = Size(particle.size, particle.size * 1.6f),
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val spin: Float,
    val color: Color,
)

@Composable
private fun StarRow(earned: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in 1..3) {
            val scale = remember { Animatable(0f) }
            LaunchedEffect(earned) {
                delay(150L * i)
                scale.animateTo(
                    1f,
                    androidx.compose.animation.core.spring(dampingRatio = 0.45f),
                )
            }
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = if (i <= earned) LabAmber else TextDim.copy(alpha = 0.35f),
                modifier = Modifier.size(44.dp).scale(scale.value),
            )
        }
    }
}

@Composable
private fun AchievementToast(
    state: GameUiState,
    onConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = state.pendingAchievements.firstOrNull()
    LaunchedEffect(current) {
        if (current != null) {
            delay(2800)
            onConsumed()
        }
    }
    AnimatedVisibility(
        visible = current != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier.padding(top = 10.dp),
    ) {
        val shown = current ?: return@AnimatedVisibility
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 6.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(shown.emoji, fontSize = 22.sp)
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(
                        "Achievement unlocked",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim,
                    )
                    Text(
                        shown.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
