package io.github.mumu12641.dolphin.ui.page.main


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = MainViewModel(), onNavigateToSettings: () -> Unit) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Dolphin") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            if (viewModel.showLog) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.saveLogToFile() },
                        icon = { Icon(Icons.Default.Save, "保存") },
                        text = { Text("保存") }
                    )
                    when (viewModel.bookingState) {
                        BookingState.RUNNING -> {
                            ExtendedFloatingActionButton(
                                onClick = { viewModel.stopBooking() },
                                icon = { Icon(Icons.Default.Stop, "停止") },
                                text = { Text("停止") }
                            )
                        }
                        else -> {
                            ExtendedFloatingActionButton(
                                onClick = { viewModel.backToForm() },
                                icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") },
                                text = { Text("返回") }
                            )
                        }
                    }
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.startBooking() },
                    icon = { Icon(Icons.Default.PlayArrow, "开始预约") },
                    text = { Text("开始预约") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            AnimatedVisibility(visible = !viewModel.showLog, enter = fadeIn(), exit = fadeOut()) {
                FormContent(viewModel)
            }
            AnimatedVisibility(visible = viewModel.showLog, enter = fadeIn(), exit = fadeOut()) {
                LogContent(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FormContent(viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // Venue Info Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "场地信息",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "场地信息", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(16.dp))

                Text(
                    "体育馆", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.venues.forEach { venue ->
                        val isSelected = viewModel.selectedVenue == venue
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onVenueSelected(venue) },
                            label = { Text(venue) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else {
                                null
                            },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                Text(
                    "场地编号 (点击选择, 顺序代表优先级)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val courtCount = viewModel.getCourtCountForSelectedVenue()
                    (1..courtCount).forEach { courtNumber ->
                        val isSelected = viewModel.selectedCourts.contains(courtNumber)
                        val priority =
                            if (isSelected) viewModel.selectedCourts.indexOf(courtNumber) + 1 else 0

                        if (isSelected) {
                            BadgedBox(
                                badge = {
                                    Badge { Text("$priority") }
                                }
                            ) {
                                Button(onClick = { viewModel.onCourtClicked(courtNumber) }) {
                                    Text("$courtNumber")
                                }
                            }
                        } else {
                            OutlinedButton(onClick = { viewModel.onCourtClicked(courtNumber) }) {
                                Text("$courtNumber")
                            }
                        }
                    }
                }
            }
        }

        // Time Slot Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = "预约时间段",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "预约时间段", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(16.dp))

                Text(
                    "选择需要预约的时间段", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    viewModel.timeSlots.forEach { timeSlot ->
                        val isSelected = viewModel.selectedTimeSlot == timeSlot
                        if (isSelected) {
                            Button(onClick = { viewModel.selectedTimeSlot = timeSlot }) {
                                Text(timeSlot)
                            }
                        } else {
                            OutlinedButton(onClick = { viewModel.selectedTimeSlot = timeSlot }) {
                                Text(timeSlot)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogContent(viewModel: MainViewModel) {
    val lazyListState = rememberLazyListState()
    LaunchedEffect(viewModel.logMessages.size) {
        if (viewModel.logMessages.isNotEmpty()) {
            lazyListState.animateScrollToItem(viewModel.logMessages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 140.dp) // Space for the FAB
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = "日志",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "日志", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(viewModel.logMessages) { logEntry ->
                    val messageColor = when (logEntry.type) {
                        LogType.INFO -> MaterialTheme.colorScheme.primary
                        LogType.ERROR -> MaterialTheme.colorScheme.error
                        LogType.WARNING -> MaterialTheme.colorScheme.tertiary
                        LogType.DEBUG -> MaterialTheme.colorScheme.secondary
                    }
                    val timestampColor = MaterialTheme.colorScheme.onSurfaceVariant

                    val annotatedString = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = timestampColor)) {
                            append("${logEntry.timestamp} ")
                        }
                        withStyle(style = SpanStyle(color = messageColor)) {
                            append(
                                if (!logEntry.exeLog) {
                                    "- ${logEntry.type} - ${logEntry.message}"
                                } else {
                                    logEntry.message
                                }
                            )
                        }
                    }

                    Text(
                        text = annotatedString,
                        modifier = Modifier.padding(4.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
