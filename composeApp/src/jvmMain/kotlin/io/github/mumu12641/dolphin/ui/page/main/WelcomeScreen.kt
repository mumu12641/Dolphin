package io.github.mumu12641.dolphin.ui.page.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mumu12641.dolphin.model.HistoryEntry
import io.github.mumu12641.dolphin.util.Constant.HISTORY_TYPE_MSG

@Composable
fun WelcomeScreen(viewModel: MainViewModel, modifier: Modifier) {
    val mainUiState by viewModel.uiState.collectAsState()
    val history = mainUiState.history
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clickable { viewModel.onAction(MainAction.StartConfig) },
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(8.dp)

        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Rounded.RocketLaunch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                    modifier = Modifier
                        .size(240.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 40.dp, y = 40.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Surface(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                        ) {
                            Text(
                                text = "准备就绪",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "创建新任务",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "配置场馆、时间与优先级策略。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "立即开始配置",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "最近配置(点击快速启动)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (history.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "暂无最近配置",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            when (history.size) {
                1 -> {
                    QuickStartCard(
                        modifier = Modifier.fillMaxWidth(),
                        historyEntry = history.first(),
                        onClick = { /* TODO: Implement quick start logic */ }
                    )
                }

                2 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        history.forEach {
                            QuickStartCard(
                                modifier = Modifier.weight(1f),
                                historyEntry = it,
                                onClick = { /* TODO: Implement quick start logic */ }
                            )
                        }
                    }
                }

                3 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        history.take(2).forEach {
                            QuickStartCard(
                                modifier = Modifier.weight(1f),
                                historyEntry = it,
                                onClick = { /* TODO: Implement quick start logic */ }
                            )
                        }
                    }
                }

                else -> { // 4 or more
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        history.take(4).chunked(2).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                rowItems.forEach { item ->
                                    QuickStartCard(
                                        modifier = Modifier.weight(1f),
                                        historyEntry = item,
                                        onClick = { /* TODO: Implement quick start logic */ }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStartCard(
    modifier: Modifier = Modifier,
    historyEntry: HistoryEntry,
    onClick: () -> Unit
) {
    val containerColor =
        if (historyEntry.status == 0) MaterialTheme.colorScheme.primaryContainer else if (historyEntry.status == 1) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor =
        if (historyEntry.status == 0) MaterialTheme.colorScheme.onPrimaryContainer else if (historyEntry.status == 1) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val badgeContainerColor =
        if (historyEntry.status == 0) MaterialTheme.colorScheme.primary else if (historyEntry.status == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val badgeContentColor =
        if (historyEntry.status == 0) MaterialTheme.colorScheme.onPrimary else if (historyEntry.status == 1) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.surfaceVariant

    val msg = HISTORY_TYPE_MSG[historyEntry.status]!!
    val icon =
        if (historyEntry.status == 0) Icons.Rounded.CheckCircle else if (historyEntry.status == 1) Icons.Rounded.Cancel else Icons.Rounded.Block

    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor, contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, Color.Black.copy(alpha = 0.05f))
                ) {
                    Text(
                        text = historyEntry.venue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = badgeContainerColor,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = badgeContentColor
                        )
                        Text(
                            text = msg,
                            color = badgeContentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = historyEntry.timeSlot,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AdsClick,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = contentColor.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "优先级: ${historyEntry.priority.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
