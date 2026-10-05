package io.github.mumu12641.dolphin.ui.page.main

import RunningScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
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
            if (bookingState == BookingState.CONFIG) {
                LargeTopAppBar(
                    title = {
                        Text(
                            if (mainUiState.configStep == BookingStep.REVIEW) "确认预约" else "Dolphin",
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onAction(MainAction.BackToHome) }) {
                            Icon(Icons.Default.Home, contentDescription = "返回首页")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    }
                )
            } else {
                LargeTopAppBar(
                    title = {
                        Row(modifier = Modifier.padding(start = 16.dp)) {
                            Text("欢迎使用Dolphin🐬")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            AnimatedContent(
                targetState = bookingState,
                transitionSpec = {
                    (scaleIn() + fadeIn()).togetherWith(scaleOut() + fadeOut())
                },
                label = "FabAnimation"
            ) { state ->
                when (state) {
                    BookingState.IDLE -> {
                        Spacer(Modifier.size(1.dp))
                    }

                    BookingState.CONFIG -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (mainUiState.configStep != BookingStep.VENUE) {
                                ExtendedFloatingActionButton(
                                    onClick = viewModel::previousConfigStep,
                                    icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                                    text = { Text("上一步") },
                                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            if (mainUiState.configStep == BookingStep.REVIEW) {
                                ExtendedFloatingActionButton(
                                    onClick = viewModel::startBooking,
                                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                    text = { Text("开始预约") }
                                )
                            } else {
                                ExtendedFloatingActionButton(
                                    onClick = viewModel::nextConfigStep,
                                    icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                                    text = { Text("继续") }
                                )
                            }
                        }
                    }

                    BookingState.RUNNING -> {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.onAction(MainAction.StopBooking) },
                            icon = { Icon(Icons.Default.Stop, "停止") },
                            text = { Text("停止预约") }
                        )
                    }

                    BookingState.ABORT, BookingState.FAILED, BookingState.SUCCESS -> Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.saveLogToFile() },
                            icon = { Icon(Icons.Default.Save, "保存日志") },
                            text = { Text("保存日志") }
                        )
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.onAction(MainAction.BackToHome) },
                            icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回主页") },
                            text = { Text("返回主页") }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = bookingState,
            modifier = Modifier.padding(paddingValues),
            contentKey = { state ->
                when (state) {
                    BookingState.IDLE -> "IDLE"
                    BookingState.CONFIG -> "CONFIG"
                    else -> "RunningGroup"
                }
            },
            transitionSpec = {
                if (targetState == BookingState.IDLE) {
                    (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300)))
                } else {
                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300)))
                }
            },
            label = "MainContentAnimation"
        ) { state ->
            when (state) {
                BookingState.IDLE -> WelcomeScreen(
                    viewModel,
                )

                BookingState.CONFIG -> BookingFlowScreen(
                    state = mainUiState,
                    viewModel = viewModel
                )

                BookingState.RUNNING, BookingState.ABORT, BookingState.FAILED, BookingState.SUCCESS ->
                    RunningScreen(viewModel)
            }
        }
    }
}
