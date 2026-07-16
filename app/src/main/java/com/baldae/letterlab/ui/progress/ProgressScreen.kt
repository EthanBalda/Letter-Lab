package com.baldae.letterlab.ui.progress

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baldae.letterlab.achievements.AchievementBook
import com.baldae.letterlab.appContainer
import com.baldae.letterlab.data.Stats
import com.baldae.letterlab.ui.theme.LabAmber
import com.baldae.letterlab.ui.theme.LabTeal
import com.baldae.letterlab.ui.theme.TextDim
import kotlinx.coroutines.flow.map

@Composable
fun ProgressScreen(onBack: () -> Unit) {
    val container = LocalContext.current.appContainer()
    val catalog = container.levelRepository.catalog

    val stats by container.statsRepository.stats
        .collectAsStateWithLifecycle(initialValue = Stats())
    val unlockedAchievements by container.achievementsRepository.unlocked
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val levelSummary by remember {
        container.progressRepository.progress.map { progress ->
            val completed = catalog.allLevels.count { progress[it.id]?.completed == true }
            val stars = catalog.allLevels.sumOf { progress[it.id]?.stars ?: 0 }
            completed to stars
        }
    }.collectAsStateWithLifecycle(initialValue = 0 to 0)

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        "Levels",
                        "${levelSummary.first}/${catalog.allLevels.size}",
                        Modifier.weight(1f),
                    )
                    StatCard(
                        "Stars",
                        "${levelSummary.second}/${catalog.maxStars}",
                        Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Moves", stats.totalMoves.toString(), Modifier.weight(1f))
                    StatCard("Wins", stats.totalWins.toString(), Modifier.weight(1f))
                    StatCard("Undos", stats.totalUndos.toString(), Modifier.weight(1f))
                }
            }
            item {
                Text(
                    "Achievements  •  ${unlockedAchievements.size}/${AchievementBook.all.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LabTeal,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            items(AchievementBook.all.size) { index ->
                val achievement = AchievementBook.all[index]
                val unlocked = achievement.id in unlockedAchievements
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(
                        alpha = if (unlocked) 0.85f else 0.45f
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (unlocked) achievement.emoji else "🔒",
                            fontSize = 24.sp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                achievement.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (unlocked) MaterialTheme.colorScheme.onSurface
                                else TextDim,
                            )
                            Text(
                                achievement.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDim,
                            )
                        }
                        if (unlocked) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = LabAmber,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        modifier = modifier.height(72.dp),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextDim)
        }
    }
}
