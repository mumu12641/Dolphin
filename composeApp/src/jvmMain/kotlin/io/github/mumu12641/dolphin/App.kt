package io.github.mumu12641.dolphin

import androidx.compose.runtime.Composable
import io.github.mumu12641.dolphin.ui.page.main.MainScreen
import io.github.mumu12641.dolphin.ui.page.main.MainViewModel
import io.github.mumu12641.dolphin.ui.page.settings.SettingsScreen
import io.github.mumu12641.dolphin.ui.page.settings.SettingsViewModel
import io.github.mumu12641.dolphin.ui.theme.DolphinTheme
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.rememberNavigator
import moe.tlaster.precompose.viewmodel.viewModel


@Composable
fun App() {
    val navigator = rememberNavigator()

    DolphinTheme {
        NavHost(
            navigator = navigator,
            initialRoute = "/main"
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
