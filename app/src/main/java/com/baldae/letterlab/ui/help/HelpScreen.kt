package com.baldae.letterlab.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baldae.letterlab.domain.Cell
import com.baldae.letterlab.ui.LetterBook
import com.baldae.letterlab.ui.components.LetterTile
import com.baldae.letterlab.ui.components.MiniTileRow
import com.baldae.letterlab.ui.theme.LabTeal
import com.baldae.letterlab.ui.theme.TextDim

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "How to Play",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                HelpCard {
                    Text(
                        "The Goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LabTeal,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Rearrange the board until every row and every column reads in " +
                            "alphabetical order. Ascending or descending both count, and " +
                            "blank cells are skipped.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MiniTileRow("abcc")
                        Text("  and  ", color = TextDim)
                        MiniTileRow("cbba")
                        Text("  both sort", color = TextDim,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                HelpCard {
                    Text(
                        "Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LabTeal,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tap a letter to pick it up, then tap a highlighted cell to act. " +
                            "Tap the letter again to put it down — that's free. " +
                            "l and m act the instant you select them.\n\n" +
                            "Undo is unlimited and every level can be restarted. " +
                            "Fewer moves earn more stars.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                Text(
                    "The Alphabet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LabTeal,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(LetterBook.entries.size) { index ->
                val info = LetterBook.entries[index]
                HelpCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LetterTile(cell = Cell(info.letter), size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                info.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                info.blurb,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    info.example?.let { (before, after) ->
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MiniTileRow(before)
                            Text(
                                "  →  ",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextDim,
                            )
                            MiniTileRow(after)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
