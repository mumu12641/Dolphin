package io.github.mumu12641.dolphin

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import io.github.mumu12641.dolphin.ui.page.main.MainScreen
import io.github.mumu12641.dolphin.ui.page.main.MainViewModel
import io.github.mumu12641.dolphin.ui.page.settings.SettingsScreen
import io.github.mumu12641.dolphin.ui.page.settings.SettingsViewModel
import io.github.mumu12641.dolphin.ui.theme.DolphinTheme
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.rememberNavigator
import moe.tlaster.precompose.navigation.transition.NavTransition
import moe.tlaster.precompose.viewmodel.viewModel


@Composable
fun App() {
    val navigator = rememberNavigator()

    DolphinTheme {
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
