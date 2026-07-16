package com.baldae.letterlab.ui.levels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baldae.letterlab.ui.theme.LabAmber
import com.baldae.letterlab.ui.theme.LabTeal
import com.baldae.letterlab.ui.theme.TextDim

@Composable
fun LevelSelectScreen(
    viewModel: LevelSelectViewModel,
    onBack: () -> Unit,
    onLevel: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Levels",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Default.Star, contentDescription = null, tint = LabAmber)
            Spacer(Modifier.width(4.dp))
            Text(
                "${state.totalStars}/${state.maxStars}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 12.dp),
            )
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (section in state.worlds) {
                item(key = "world_${section.world.id}") {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            section.world.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LabTeal,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${section.completedCount}/${section.items.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextDim,
                        )
                    }
                }
                // Chunk so future worlds with more than five levels wrap cleanly.
                for ((chunkIndex, chunk) in section.items.chunked(5).withIndex()) {
                    item(key = "levels_${section.world.id}_$chunkIndex") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (item in chunk) {
                                LevelChip(
                                    item = item,
                                    modifier = Modifier.weight(1f),
                                    onClick = { if (!item.locked) onLevel(item.def.id) },
                                )
                            }
                            repeat(5 - chunk.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelChip(item: LevelItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val borderColor = when {
        item.locked -> Color.White.copy(alpha = 0.08f)
        item.completed -> LabTeal.copy(alpha = 0.7f)
        else -> Color.White.copy(alpha = 0.35f)
    }
    val description = buildString {
        append("Level ${item.def.id}, ${item.def.name}")
        when {
            item.locked -> append(", locked")
            item.completed -> append(", completed with ${item.stars} stars")
            item.inProgress -> append(", in progress")
        }
    }
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (item.locked) 0.4f else 0.85f),
        modifier = modifier
            .height(72.dp)
            .clip(shape)
            .border(1.5.dp, borderColor, shape)
            .semantics { contentDescription = description }
            .clickable(enabled = !item.locked, onClick = onClick),
    ) {
        Box(Modifier.fillMaxSize()) {
        if (item.inProgress && !item.locked) {
            // Small dot marking a resumable level.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LabAmber),
            )
        }
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (item.locked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = TextDim.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    item.def.id.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    for (i in 1..3) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i <= item.stars) LabAmber
                            else TextDim.copy(alpha = 0.25f),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                item.bestMoves?.let {
                    Text(
                        "best $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim,
                    )
                }
            }
        }
        }
    }
}
