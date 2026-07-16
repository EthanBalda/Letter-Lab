package com.baldae.letterlab.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.baldae.letterlab.appContainer
import com.baldae.letterlab.data.Settings
import com.baldae.letterlab.ui.background.AnimatedBackground
import com.baldae.letterlab.ui.background.BackgroundTheme
import com.baldae.letterlab.ui.game.GameScreen
import com.baldae.letterlab.ui.game.GameViewModel
import com.baldae.letterlab.ui.help.HelpScreen
import com.baldae.letterlab.ui.levels.LevelSelectScreen
import com.baldae.letterlab.ui.levels.LevelSelectViewModel
import com.baldae.letterlab.ui.menu.MainMenuScreen
import com.baldae.letterlab.ui.progress.ProgressScreen
import com.baldae.letterlab.ui.settings.SettingsScreen
import com.baldae.letterlab.ui.settings.SettingsViewModel

object Routes {
    const val MENU = "menu"
    const val LEVELS = "levels"
    const val GAME = "game/{levelId}"
    const val HELP = "help"
    const val SETTINGS = "settings"
    const val PROGRESS = "progress"

    fun game(levelId: Int) = "game/$levelId"
}

@Composable
fun LetterLabNavHost() {
    val container = LocalContext.current.appContainer()
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = Settings())
    val navController = rememberNavController()

    Box(Modifier.fillMaxSize()) {
        // One shared, animated backdrop behind every screen.
        AnimatedBackground(BackgroundTheme.fromId(settings.themeId))

        NavHost(
            navController = navController,
            startDestination = Routes.MENU,
            // Gentle rise + fade; the shared animated backdrop never moves,
            // so screens feel like panels floating above the lab.
            enterTransition = {
                fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 24 }
            },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = {
                fadeIn(tween(280)) + slideInVertically(tween(280)) { -it / 32 }
            },
            popExitTransition = {
                fadeOut(tween(180)) + slideOutVertically(tween(220)) { it / 24 }
            },
        ) {
            composable(Routes.MENU) {
                MainMenuScreen(
                    onPlay = { navController.navigate(Routes.LEVELS) },
                    onHelp = { navController.navigate(Routes.HELP) },
                    onProgress = { navController.navigate(Routes.PROGRESS) },
                    onSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.LEVELS) {
                val vm: LevelSelectViewModel =
                    viewModel(factory = LevelSelectViewModel.factory(container))
                LevelSelectScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onLevel = { id -> navController.navigate(Routes.game(id)) },
                )
            }
            composable(
                Routes.GAME,
                arguments = listOf(navArgument("levelId") { type = NavType.IntType }),
            ) { entry ->
                val levelId = remember(entry) {
                    entry.arguments?.getInt("levelId") ?: 1
                }
                val vm: GameViewModel = viewModel(
                    key = "game_$levelId",
                    factory = GameViewModel.factory(container, levelId),
                )
                GameScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onNextLevel = { nextId ->
                        navController.navigate(Routes.game(nextId)) {
                            popUpTo(Routes.LEVELS)
                        }
                    },
                )
            }
            composable(Routes.HELP) {
                HelpScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel =
                    viewModel(factory = SettingsViewModel.factory(container))
                SettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
            composable(Routes.PROGRESS) {
                ProgressScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
