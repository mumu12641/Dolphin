package io.github.mumu12641.dolphin

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dolphin.composeapp.generated.resources.Res
import dolphin.composeapp.generated.resources.icon
import moe.tlaster.precompose.PreComposeApp
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Dolphin",
        state = rememberWindowState(width = 800.dp, height = 800.dp),
        resizable = false,
        icon = painterResource(Res.drawable.icon)
    ) {
        PreComposeApp {
            App()
        }
    }
}
