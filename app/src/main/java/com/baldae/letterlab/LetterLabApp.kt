package com.baldae.letterlab

import android.app.Application
import android.content.Context
import com.baldae.letterlab.audio.HapticsManager
import com.baldae.letterlab.audio.MusicManager
import com.baldae.letterlab.audio.SoundManager
import com.baldae.letterlab.data.AchievementsRepository
import com.baldae.letterlab.data.LevelRepository
import com.baldae.letterlab.data.ProgressRepository
import com.baldae.letterlab.data.SettingsRepository
import com.baldae.letterlab.data.StatsRepository
import com.baldae.letterlab.data.gameDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Plain-constructor service locator; small enough that a DI framework would be noise. */
class AppContainer(context: Context) {
    private val dataStore = context.gameDataStore

    val levelRepository = LevelRepository(context)
    val progressRepository = ProgressRepository(dataStore)
    val settingsRepository = SettingsRepository(dataStore)
    val statsRepository = StatsRepository(dataStore)
    val achievementsRepository = AchievementsRepository(dataStore)
    val soundManager = SoundManager(context)
    val musicManager = MusicManager()
    val haptics = HapticsManager(context)
}

class LetterLabApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            container.settingsRepository.settings.collect {
                container.soundManager.enabled = it.soundEnabled
                container.musicManager.setEnabled(it.musicEnabled)
                container.haptics.enabled = it.hapticsEnabled
            }
        }
    }
}

fun Context.appContainer(): AppContainer =
    (applicationContext as LetterLabApp).container
