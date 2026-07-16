package com.baldae.letterlab.ui.settings

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baldae.letterlab.ui.background.BackgroundTheme
import com.baldae.letterlab.ui.theme.Danger
import com.baldae.letterlab.ui.theme.LabTeal
import com.baldae.letterlab.ui.theme.TextDim

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingsCard {
                ToggleRow("Sound effects", state.settings.soundEnabled, viewModel::setSound)
                ToggleRow("Music", state.settings.musicEnabled, viewModel::setMusic)
                ToggleRow("Haptics", state.settings.hapticsEnabled, viewModel::setHaptics)
            }

            Text(
                "Background theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LabTeal,
                modifier = Modifier.padding(top = 8.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(((state.themes.size + 1) / 2 * 106).dp),
                userScrollEnabled = false,
            ) {
                items(state.themes) { option ->
                    ThemeCard(
                        option = option,
                        selected = option.theme.id == state.settings.themeId,
                        onClick = { viewModel.selectTheme(option) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            SettingsCard {
                Row(
                    Modifier.fillMaxWidth().clickable { confirmReset = true }.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Reset progress", color = Danger, fontWeight = FontWeight.Bold)
                        Text(
                            "Clears levels, stars, stats and achievements. Settings are kept.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDim,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset all progress?") },
            text = { Text("Every level, star, statistic and achievement will be wiped. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetProgress()
                    confirmReset = false
                }) { Text("Reset", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), content = content)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    // The whole row is one toggleable for screen readers and fat fingers alike.
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ThemeCard(option: ThemeOption, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) LabTeal else Color.White.copy(alpha = 0.12f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Brush.linearGradient(swatch(option.theme))),
            contentAlignment = Alignment.Center,
        ) {
            if (!option.unlocked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            option.theme.displayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            if (option.unlocked && !option.theme.requiresAll && option.theme.unlockWorldId == null)
                "Classic" else option.unlockDescription,
            style = MaterialTheme.typography.labelSmall,
            color = TextDim,
            maxLines = 1,
        )
        // Selected indicator dot for color-blind friendliness beyond the border.
        if (selected) {
            Spacer(Modifier.height(4.dp))
            Box(Modifier.size(6.dp).clip(CircleShape).background(LabTeal))
        }
    }
}

private fun swatch(theme: BackgroundTheme): List<Color> = when (theme) {
    BackgroundTheme.LABORATORY -> listOf(Color(0xFF0E1120), Color(0xFF3A2E7A), Color(0xFF17564F))
    BackgroundTheme.AURORA -> listOf(Color(0xFF0E1120), Color(0xFF1D8C7C), Color(0xFF5C4DB8))
    BackgroundTheme.GEOMETRY -> listOf(Color(0xFF10132A), Color(0xFF2A2F5A), Color(0xFF444C8C))
    BackgroundTheme.NEBULA -> listOf(Color(0xFF0B0D1A), Color(0xFF3A2560), Color(0xFF11463F))
    BackgroundTheme.EMBER -> listOf(Color(0xFF1A1030), Color(0xFF7A2E28), Color(0xFFB35A2E))
    BackgroundTheme.GILDED -> listOf(Color(0xFF14121C), Color(0xFF6A5218), Color(0xFFB08D2E))
}
