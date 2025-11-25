import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ArrowRight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mumu12641.dolphin.ui.page.main.BookingState
import io.github.mumu12641.dolphin.ui.page.main.LogEntry
import io.github.mumu12641.dolphin.ui.page.main.LogType
import io.github.mumu12641.dolphin.ui.page.main.MainViewModel

@Composable
fun RunningScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val mainUiState by viewModel.uiState.collectAsState()
    val venue = mainUiState.selectedVenue
    val timeSlot = mainUiState.selectedTimeSlot
    val courts = mainUiState.selectedCourts
    val state = mainUiState.bookingState
    val stateMsgEN =
        if (state == BookingState.RUNNING) "RUNNING" else if (state == BookingState.ABORT) "ABORT" else if (state == BookingState.SUCCESS) "SUCCESS" else "FAILED"
    val stateMsgZH =
        if (state == BookingState.RUNNING) "抢场中" else if (state == BookingState.ABORT) "已中止" else if (state == BookingState.SUCCESS) "成功" else "失败"
    val icon =
        if (state == BookingState.RUNNING) Icons.Rounded.Timer else if (state == BookingState.ABORT) Icons.Rounded.Block else if (state == BookingState.SUCCESS) Icons.Default.Timer else Icons.Rounded.Cancel
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                // Content
                Icon(
                    imageVector = Icons.Rounded.RocketLaunch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 20.dp, y = 20.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.alpha(0.9f)
                        ) {
                            Icon(
                                icon,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                stateMsgEN,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            stateMsgZH,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.background
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(20.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Place,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "TARGET VENUE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        venue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(2f)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Update,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "目标时间段",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(start = 26.dp)
                        ) {
                            Text(
                                text = timeSlot,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AdsClick,
                            null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "策略:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            courts.forEachIndexed { index, i ->
                                Row(
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(20.dp)
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            RoundedCornerShape(4.dp)
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        i.toString(),
                                        fontSize = 10.sp,
                                        lineHeight = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (index < courts.size - 1) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowRight,
                                        null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = "日志",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "日志",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LogContent(mainUiState.logMessages)
            }
        }
    }
}


@Composable
fun LogContent(logMessages: List<LogEntry>) {
    val lazyListState = rememberLazyListState()
    LaunchedEffect(logMessages.size) {
        if (logMessages.isNotEmpty()) {
            lazyListState.animateScrollToItem(logMessages.lastIndex)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(logMessages) { logEntry ->
            val timestampColor = MaterialTheme.colorScheme.onSurfaceVariant
            val messageColor = when (logEntry.type) {
                LogType.INFO -> MaterialTheme.colorScheme.primary
                LogType.ERROR -> MaterialTheme.colorScheme.error
                LogType.WARNING -> MaterialTheme.colorScheme.tertiary
                LogType.DEBUG -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.primary
            }
            Row(verticalAlignment = Alignment.Top) {
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