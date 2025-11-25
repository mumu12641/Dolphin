package io.github.mumu12641.dolphin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.rememberDynamicColorScheme

@Composable
fun DolphinTheme(seedColor: Color, darkMode: Boolean, content: @Composable () -> Unit) {
    val colorScheme = rememberDynamicColorScheme(seedColor = seedColor, isDark = darkMode)

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
