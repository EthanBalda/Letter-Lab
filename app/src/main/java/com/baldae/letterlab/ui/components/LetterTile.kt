package com.baldae.letterlab.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baldae.letterlab.domain.Cell
import com.baldae.letterlab.domain.Letters
import com.baldae.letterlab.ui.LetterBook
import com.baldae.letterlab.ui.theme.LabAmber
import com.baldae.letterlab.ui.theme.letterColor
import kotlinx.coroutines.delay

enum class TileState { NORMAL, SELECTED, TARGET, HINT, DIMMED }

/**
 * One shared pulse value for all target tiles on a board. Hoisted here so a
 * board runs a single infinite animation rather than one per tile.
 */
@Composable
fun rememberTargetPulse(): State<Float> =
    rememberInfiniteTransition(label = "targetPulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
        label = "targetPulse",
    )

/**
 * One board cell. Pops softly when its content changes, glows when selected,
 * pulses when it's a legal target and glows amber when suggested by a hint.
 */
@Composable
fun LetterTile(
    cell: Cell,
    size: Dp,
    state: TileState = TileState.NORMAL,
    /**
     * Shared pulse from [rememberTargetPulse]. Passed as State and only read
     * by TARGET tiles, so the per-frame pulse recomposes just those tiles.
     */
    pulse: State<Float>? = null,
    /** Staggered entrance delay; negative disables the entrance animation. */
    appearDelayMillis: Int = -1,
    /** True while a slide animation explains this cell's change — skips the pop. */
    suppressPop: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isEmpty = cell.isEmpty
    val base = letterColor(cell.letter)

    // Pop animation when the content of this cell changes.
    val scale = remember { Animatable(1f) }
    val firstValue = remember { mutableStateOf(true) }
    LaunchedEffect(cell) {
        if (firstValue.value) {
            firstValue.value = false
        } else if (!suppressPop) {
            scale.snapTo(1.18f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    // Staggered entrance when a board first appears.
    val appear = remember {
        Animatable(if (appearDelayMillis < 0) 1f else 0f)
    }
    LaunchedEffect(Unit) {
        if (appearDelayMillis >= 0 && appear.value < 1f) {
            delay(appearDelayMillis.toLong())
            appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    val shape = RoundedCornerShape(size / 5)
    val backgroundColor = when {
        isEmpty -> Color.White.copy(alpha = 0.05f)
        state == TileState.DIMMED -> base.copy(alpha = 0.25f)
        else -> base.copy(alpha = 0.92f)
    }
    val borderColor = when (state) {
        TileState.SELECTED -> Color.White
        TileState.TARGET -> Color.White.copy(alpha = pulse?.value ?: 1f)
        TileState.HINT -> LabAmber
        else -> Color.White.copy(alpha = if (isEmpty) 0.08f else 0.15f)
    }
    val borderWidth = when (state) {
        TileState.SELECTED, TileState.HINT -> 2.5.dp
        TileState.TARGET -> 2.dp
        else -> 1.dp
    }

    val description = buildString {
        if (isEmpty) append("Empty cell") else {
            append("Letter ${cell.letter}")
            LetterBook.of(cell.letter)?.let { append(", ${it.name}") }
            cell.held?.let { if (it != Letters.EMPTY) append(", holding $it") }
        }
        when (state) {
            TileState.SELECTED -> append(", selected")
            TileState.TARGET -> append(", possible target")
            TileState.HINT -> append(", suggested move")
            else -> Unit
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(
                (if (state == TileState.SELECTED) scale.value * 1.08f else scale.value) *
                    appear.value
            )
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .semantics { contentDescription = description }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!isEmpty) {
            val textAlpha = if (state == TileState.DIMMED) 0.5f else 1f
            Text(
                text = cell.letter.toString(),
                fontSize = (size.value * 0.48f).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10132A).copy(alpha = textAlpha),
            )
            // Badge showing what an h is holding.
            cell.held?.let { held ->
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(size / 16)
                        .size(size / 3)
                        .clip(RoundedCornerShape(size / 12))
                        .background(
                            if (held == Letters.EMPTY) Color.Black.copy(alpha = 0.25f)
                            else letterColor(held)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = held.toString(),
                        fontSize = (size.value * 0.2f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10132A),
                    )
                }
            }
        }
    }
}

/** A tiny read-only row of tiles for examples in the help screen. */
@Composable
fun MiniTileRow(text: String, tileSize: Dp = 26.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (ch in text) {
            LetterTile(cell = Cell(ch), size = tileSize)
        }
    }
}
