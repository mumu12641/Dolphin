package io.github.mumu12641.dolphin.ui.page.main


import RunningScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = MainViewModel(), onNavigateToSettings: () -> Unit) {
    val mainUiState by viewModel.uiState.collectAsState()
    val bookingState = mainUiState.bookingState

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(modifier = Modifier.padding(start = 24.dp)) {
                        Text("Welcome to Dolphin🐬")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            when (bookingState) {
                BookingState.IDLE -> {

                }

                BookingState.CONFIG -> {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                viewModel.onAction(MainAction.BackToHome)
                            },
                            icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") },
                            text = { Text("返回主页") }
                        )
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.startBooking() },
                            icon = { Icon(Icons.Default.PlayArrow, "开始") },
                            text = { Text("开始预约") }
                        )
                    }
                }

                BookingState.RUNNING -> {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.onAction(MainAction.StopBooking) },
                        icon = { Icon(Icons.Default.Stop, "停止") },
                        text = { Text("停止预约") }
                    )
                }

                BookingState.STOPPED -> Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.saveLogToFile() },
                        icon = { Icon(Icons.Default.Save, "保存日志") },
                        text = { Text("保存日志") }
                    )
                    ExtendedFloatingActionButton(
                        onClick = {
                            viewModel.onAction(MainAction.BackToHome)

                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回主页") },
                        text = { Text("返回主页") }
                    )

                }
            }

        }
    ) { paddingValues ->
        when (bookingState) {
            BookingState.IDLE -> WelcomeScreen(
                viewModel,
                modifier = Modifier.padding(paddingValues)
            )

            BookingState.CONFIG -> ConfigScreen(
                viewModel,
                modifier = Modifier.padding(paddingValues)
            )

            BookingState.RUNNING -> RunningScreen(
                viewModel,
                modifier = Modifier.padding(paddingValues)
            )

            BookingState.STOPPED -> RunningScreen(
                viewModel,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}