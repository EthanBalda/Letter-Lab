package com.baldae.letterlab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// LetterLab is deliberately dark-first: the animated backgrounds are built
// around a deep-navy canvas that keeps tile contrast high in both system modes.

val DeepSpace = Color(0xFF0E1120)
val PanelNavy = Color(0xFF161A30)
val PanelNavyHigh = Color(0xFF1E2340)
val LabViolet = Color(0xFF8B7CF6)
val LabTeal = Color(0xFF43D9C4)
val LabAmber = Color(0xFFFFC857)
val TextBright = Color(0xFFEDEFFB)
val TextDim = Color(0xFF9AA0BC)
val Danger = Color(0xFFEF5350)

private val LabColorScheme = darkColorScheme(
    primary = LabViolet,
    onPrimary = Color(0xFF14102E),
    secondary = LabTeal,
    onSecondary = Color(0xFF04211D),
    tertiary = LabAmber,
    onTertiary = Color(0xFF2B2005),
    background = DeepSpace,
    onBackground = TextBright,
    surface = PanelNavy,
    onSurface = TextBright,
    surfaceVariant = PanelNavyHigh,
    onSurfaceVariant = TextDim,
    error = Danger,
    outline = Color(0xFF3A4066),
)

/** Per-letter tile colors, shared by the board, the help screen and menus. */
fun letterColor(letter: Char): Color = when (letter) {
    'a' -> Color(0xFF788097)
    'b' -> Color(0xFF4FC3F7)
    'c' -> Color(0xFF9575CD)
    'd' -> Color(0xFFEF5350)
    'e' -> Color(0xFF66BB6A)
    'f' -> Color(0xFFFFA726)
    'g' -> Color(0xFF26A69A)
    'h' -> Color(0xFFEC407A)
    'i' -> Color(0xFF546E7A)
    'j' -> Color(0xFF7986CB)
    'k' -> Color(0xFFFFCA28)
    'l' -> Color(0xFFF48FB1)
    'm' -> Color(0xFF9C6ADE)
    else -> Color(0xFF2A2F4A) // blank slot
}

@Composable
fun LetterLabTheme(content: @Composable () -> Unit) {
    // Same scheme in light and dark system modes — the game has its own look.
    MaterialTheme(
        colorScheme = LabColorScheme,
        content = content,
    )
}
