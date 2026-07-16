package com.baldae.letterlab.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baldae.letterlab.AppContainer
import com.baldae.letterlab.data.Settings
import com.baldae.letterlab.ui.background.BackgroundTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ThemeOption(
    val theme: BackgroundTheme,
    val unlocked: Boolean,
    val unlockDescription: String,
)

data class SettingsUiState(
    val settings: Settings = Settings(),
    val themes: List<ThemeOption> = emptyList(),
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val catalog = container.levelRepository.catalog

    val state: StateFlow<SettingsUiState> = combine(
        container.settingsRepository.settings,
        container.progressRepository.progress,
    ) { settings, progress ->
        SettingsUiState(
            settings = settings,
            themes = BackgroundTheme.entries.map { theme ->
                ThemeOption(
                    theme = theme,
                    unlocked = BackgroundTheme.isUnlocked(theme, catalog, progress),
                    unlockDescription = theme.unlockDescription(catalog),
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setSound(enabled: Boolean) =
        viewModelScope.launch { container.settingsRepository.setSoundEnabled(enabled) }

    fun setMusic(enabled: Boolean) =
        viewModelScope.launch { container.settingsRepository.setMusicEnabled(enabled) }

    fun setHaptics(enabled: Boolean) =
        viewModelScope.launch { container.settingsRepository.setHapticsEnabled(enabled) }

    fun selectTheme(option: ThemeOption) {
        if (!option.unlocked) return
        viewModelScope.launch { container.settingsRepository.setTheme(option.theme.id) }
    }

    fun resetProgress() = viewModelScope.launch {
        container.progressRepository.resetAllProgress()
        // Fall back to the default theme in case the selected one is now locked.
        container.settingsRepository.setTheme(BackgroundTheme.LABORATORY.id)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(container) as T
            }
    }
}
