package com.baldae.letterlab.ui.menu

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baldae.letterlab.appContainer
import com.baldae.letterlab.domain.Cell
import com.baldae.letterlab.ui.components.LetterTile
import com.baldae.letterlab.ui.theme.LabAmber
import com.baldae.letterlab.ui.theme.TextDim
import kotlinx.coroutines.flow.map

@Composable
fun MainMenuScreen(
    onPlay: () -> Unit,
    onHelp: () -> Unit,
    onProgress: () -> Unit,
    onSettings: () -> Unit,
) {
    val container = LocalContext.current.appContainer()
    val catalog = container.levelRepository.catalog
    val totalStars by remember {
        container.progressRepository.progress.map { progress ->
            catalog.allLevels.sumOf { progress[it.id]?.stars ?: 0 }
        }
    }.collectAsStateWithLifecycle(initialValue = 0)

    val breathe by rememberInfiniteTransition(label = "title").animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "title",
    )

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Decorative sample of the alphabet.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (ch in "bcdefg") {
                LetterTile(cell = Cell(ch), size = 34.dp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "LetterLab",
            fontSize = 46.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            // graphicsLayer reads the animation in the draw phase, so the
            // breathing title never recomposes the menu.
            modifier = Modifier.graphicsLayer {
                scaleX = breathe
                scaleY = breathe
            },
        )
        Text(
            "a letter-sorting puzzle",
            style = MaterialTheme.typography.bodyMedium,
            color = TextDim,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = LabAmber)
            Spacer(Modifier.width(6.dp))
            Text(
                "$totalStars / ${catalog.maxStars} stars",
                style = MaterialTheme.typography.labelLarge,
                color = TextDim,
            )
        }
        Spacer(Modifier.height(44.dp))

        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text("Play", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onHelp, modifier = Modifier.fillMaxWidth()) {
            Text("How to Play")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onProgress, modifier = Modifier.fillMaxWidth()) {
            Text("Progress")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Settings")
        }
    }
}
