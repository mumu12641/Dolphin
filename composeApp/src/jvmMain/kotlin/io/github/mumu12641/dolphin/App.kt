package io.github.mumu12641.dolphin

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import io.github.mumu12641.dolphin.data.PreferencesRepository
import io.github.mumu12641.dolphin.ui.page.main.MainScreen
import io.github.mumu12641.dolphin.ui.page.main.MainViewModel
import io.github.mumu12641.dolphin.ui.page.settings.SettingsScreen
import io.github.mumu12641.dolphin.ui.page.settings.SettingsViewModel
import io.github.mumu12641.dolphin.ui.page.settings.toComposeColor
import io.github.mumu12641.dolphin.ui.theme.DolphinTheme
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.rememberNavigator
import moe.tlaster.precompose.navigation.transition.NavTransition
import moe.tlaster.precompose.viewmodel.viewModel


@Composable
fun App() {
    val navigator = rememberNavigator()
    val theme by PreferencesRepository.themeFlow
        .collectAsState(initial = Pair("0x89CFF0", "false"))
    val seedColor = remember(theme) {
        theme.first.toComposeColor()
    }
    val darkMode = remember(theme) {
        theme.second.toBoolean()
    }
    DolphinTheme(seedColor = seedColor, darkMode = darkMode) {
        val slideTransition = NavTransition(
            createTransition = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(tween(300)),

            pauseTransition = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300)
            ) + fadeOut(tween(300)),

            resumeTransition = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300)
            ) + fadeIn(tween(300)),

            destroyTransition = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(tween(300))
        )

        NavHost(
            navigator = navigator,
            initialRoute = "/main",
            navTransition = slideTransition
        ) {
            scene("/main") {
                val mainViewModel = viewModel(modelClass = MainViewModel::class) {
                    MainViewModel()
                }
                MainScreen(viewModel = mainViewModel, onNavigateToSettings = {
                    navigator.navigate("/settings")
                })
            }
            scene("/settings") {
                val settingsViewModel = viewModel(modelClass = SettingsViewModel::class) {
                    SettingsViewModel()
                }
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateUp = {
                        navigator.goBack()
                    }
                )
            }
        }
    }
}
