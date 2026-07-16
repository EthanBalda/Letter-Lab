package com.baldae.letterlab.ui.background

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.baldae.letterlab.ui.theme.DeepSpace
import kotlin.math.PI
import kotlin.math.sin

/**
 * Fully offline, procedurally animated backdrops. Premium but quiet: low
 * alpha, slow motion, and a scrim so the board always keeps its contrast.
 */
@Composable
fun AnimatedBackground(theme: BackgroundTheme, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(DeepSpace)) {
        Crossfade(theme, animationSpec = tween(900), label = "bg") { t ->
            when (t) {
                BackgroundTheme.LABORATORY -> LaboratoryBackground()
                BackgroundTheme.AURORA -> AuroraBackground()
                BackgroundTheme.GEOMETRY -> GeometryBackground()
                BackgroundTheme.NEBULA -> NebulaBackground()
                BackgroundTheme.EMBER -> EmberBackground()
                BackgroundTheme.GILDED -> GildedBackground()
            }
        }
        // Gentle scrim keeps gameplay legible over any theme.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to DeepSpace.copy(alpha = 0.20f),
                    1f to DeepSpace.copy(alpha = 0.45f),
                )
            )
        )
    }
}

/** A slow 0→1 loop shared by the renderers. */
@Composable
private fun drift(durationMillis: Int, label: String): State<Float> =
    rememberInfiniteTransition(label = label).animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = label,
    )

/** Deterministic pseudo-random in [0,1) from a seed pair — no allocations per frame. */
private fun rand(seed: Int, salt: Int): Float {
    var h = seed * 374761393 + salt * 668265263
    h = (h xor (h shr 13)) * 1274126177
    return ((h xor (h shr 16)) and 0x7FFFFFFF) / 2147483647f
}

private fun DrawScope.glowBlob(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

// --------------------------------------------------------------- Laboratory

@Composable
private fun LaboratoryBackground() {
    val t by drift(90_000, "lab")
    // Cache above the distinct glyph count so per-frame text layout is free.
    val measurer = rememberTextMeasurer(cacheSize = 16)
    val glyphStyle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Drifting glow blobs.
        glowBlob(
            Offset(w * (0.25f + 0.12f * sin(t * 2 * PI).toFloat()), h * 0.22f),
            w * 0.55f, Color(0xFF8B7CF6).copy(alpha = 0.10f),
        )
        glowBlob(
            Offset(w * 0.85f, h * (0.65f + 0.08f * sin((t + 0.3f) * 2 * PI).toFloat())),
            w * 0.6f, Color(0xFF43D9C4).copy(alpha = 0.08f),
        )
        glowBlob(
            Offset(w * 0.1f, h * 0.9f),
            w * 0.5f, Color(0xFF4FC3F7).copy(alpha = 0.06f),
        )
        // Faint letters rising like bubbles in a beaker.
        val letters = "abcdefghjklm"
        for (i in 0 until 14) {
            val speed = 0.4f + rand(i, 1) * 0.8f
            val y = (1f + rand(i, 2) - t * speed).mod(1.2f) - 0.1f
            val x = rand(i, 3) + 0.03f * sin((t * 3 + rand(i, 4)) * 2 * PI).toFloat()
            val alpha = 0.04f + rand(i, 5) * 0.05f
            val topLeft = Offset(x * w, y * h)
            // drawText lays out within the remaining canvas; skip glyphs that
            // have drifted too close to (or past) the edges.
            val margin = 80f
            if (topLeft.x in 0f..(w - margin) && topLeft.y in 0f..(h - margin)) {
                drawText(
                    textMeasurer = measurer,
                    text = letters[i % letters.length].toString(),
                    topLeft = topLeft,
                    style = glyphStyle.copy(color = Color.White.copy(alpha = alpha)),
                )
            }
        }
    }
}

// ------------------------------------------------------------------- Aurora

@Composable
private fun AuroraBackground() {
    val t by drift(60_000, "aurora")
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val bands = listOf(
            Triple(Color(0xFF43D9C4), 0.30f, 1f),
            Triple(Color(0xFF8B7CF6), 0.45f, -1.4f),
            Triple(Color(0xFF66BB6A), 0.60f, 0.7f),
        )
        for ((band, base, dir) in bands) {
            val path = Path()
            path.moveTo(0f, h)
            var x = 0f
            while (x <= w) {
                val phase = (t * dir + x / w) * 2 * PI
                val y = h * base + h * 0.06f * sin(phase * 2).toFloat() +
                    h * 0.03f * sin(phase * 5 + 1.7).toFloat()
                path.lineTo(x, y)
                x += w / 24f
            }
            path.lineTo(w, h)
            path.close()
            drawPath(
                path,
                brush = Brush.verticalGradient(
                    0f to band.copy(alpha = 0.16f),
                    1f to Color.Transparent,
                    startY = h * (base - 0.1f),
                    endY = h,
                ),
            )
        }
    }
}

// ----------------------------------------------------------------- Geometry

@Composable
private fun GeometryBackground() {
    val t by drift(80_000, "geometry")
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val step = w / 5f
        var row = 0
        var y = step / 2
        while (y < h + step) {
            var col = 0
            var x = if (row % 2 == 0) step / 2 else step
            while (x < w + step) {
                val wobble = sin((t * 2 + rand(row, col)) * 2 * PI).toFloat()
                val sizePx = step * (0.28f + 0.05f * wobble)
                rotate(45f + wobble * 8f, pivot = Offset(x, y)) {
                    drawRect(
                        color = Color(0xFF8B7CF6).copy(alpha = 0.05f + 0.02f * wobble),
                        topLeft = Offset(x - sizePx / 2, y - sizePx / 2),
                        size = androidx.compose.ui.geometry.Size(sizePx, sizePx),
                        style = Stroke(width = 1.5f),
                    )
                }
                x += step
                col++
            }
            y += step
            row++
        }
        // One large, slowly rotating hexagon-ish accent.
        rotate(t * 360f, pivot = Offset(w * 0.8f, h * 0.25f)) {
            drawCircle(
                color = Color(0xFF43D9C4).copy(alpha = 0.05f),
                radius = w * 0.3f,
                center = Offset(w * 0.8f, h * 0.25f),
                style = Stroke(width = 2f),
            )
        }
    }
}

// ------------------------------------------------------------------- Nebula

@Composable
private fun NebulaBackground() {
    val t by drift(120_000, "nebula")
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        glowBlob(
            Offset(w * (0.6f + 0.1f * sin(t * 2 * PI).toFloat()), h * 0.35f),
            w * 0.7f, Color(0xFF7E57C2).copy(alpha = 0.12f),
        )
        glowBlob(Offset(w * 0.2f, h * 0.75f), w * 0.5f, Color(0xFF26A69A).copy(alpha = 0.07f))
        // Three parallax star layers.
        for (layer in 0 until 3) {
            val speed = (layer + 1) * 0.02f
            val count = 24 - layer * 4
            val radius = 1.2f + layer * 0.9f
            for (i in 0 until count) {
                val x = (rand(i, layer * 7 + 1) + t * speed * 10f).mod(1f)
                val yPos = rand(i, layer * 7 + 2)
                val twinkle =
                    0.5f + 0.5f * sin((t * 40 * (0.5f + rand(i, layer * 7 + 3)) + i) * 2 * PI).toFloat()
                drawCircle(
                    color = Color.White.copy(alpha = (0.10f + 0.20f * twinkle) / (layer + 1)),
                    radius = radius,
                    center = Offset(x * w, yPos * h),
                )
            }
        }
    }
}

// -------------------------------------------------------------------- Ember

@Composable
private fun EmberBackground() {
    val t by drift(50_000, "ember")
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF1A1030),
                0.6f to Color(0xFF2A122A),
                1f to Color(0xFF3A1420),
            )
        )
        glowBlob(Offset(w * 0.5f, h * 1.05f), w * 0.9f, Color(0xFFFF7043).copy(alpha = 0.12f))
        // Rising embers.
        for (i in 0 until 26) {
            val speed = 0.8f + rand(i, 11) * 1.6f
            val y = (1f + rand(i, 12) - t * speed * 4f).mod(1.15f) - 0.05f
            val x = rand(i, 13) + 0.02f * sin((t * 30 + rand(i, 14) * 6) * 2 * PI).toFloat()
            val flicker = 0.5f + 0.5f * sin((t * 200 + i) * 2 * PI).toFloat()
            drawCircle(
                color = Color(0xFFFFB74D).copy(alpha = (0.10f + 0.15f * flicker) * (1f - y)),
                radius = 1.5f + rand(i, 15) * 2.5f,
                center = Offset(x * w, y * h),
            )
        }
    }
}

// ------------------------------------------------------------------- Gilded

@Composable
private fun GildedBackground() {
    val t by drift(70_000, "gilded")
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF14121C),
                1f to Color(0xFF1E1A10),
            )
        )
        // Sweeping diagonal shimmer.
        val sweep = (t * 2f).mod(1.4f) - 0.2f
        drawRect(
            brush = Brush.linearGradient(
                0f to Color.Transparent,
                0.5f to Color(0xFFFFD54F).copy(alpha = 0.05f),
                1f to Color.Transparent,
                start = Offset(w * (sweep - 0.3f), 0f),
                end = Offset(w * (sweep + 0.3f), h),
            )
        )
        // Drifting gold dust.
        for (i in 0 until 30) {
            val speed = 0.3f + rand(i, 21) * 0.8f
            val x = (rand(i, 22) + t * speed).mod(1f)
            val y = (rand(i, 23) + t * speed * 0.4f).mod(1f)
            val glint = 0.5f + 0.5f * sin((t * 60 + i * 2) * 2 * PI).toFloat()
            drawCircle(
                color = Color(0xFFFFD54F).copy(alpha = 0.06f + 0.14f * glint),
                radius = 1f + rand(i, 24) * 2f,
                center = Offset(x * w, y * h),
            )
        }
    }
}
