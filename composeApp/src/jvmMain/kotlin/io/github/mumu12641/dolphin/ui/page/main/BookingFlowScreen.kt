package io.github.mumu12641.dolphin.ui.page.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.mumu12641.dolphin.util.Constant

@Composable
fun BookingFlowScreen(state: MainUiState, viewModel: MainViewModel) {
    if (state.configStep == BookingStep.REVIEW) {
        ReviewScreen(state)
        return
    }
    val step = when (state.configStep) {
        BookingStep.VENUE -> 0
        BookingStep.TIME -> 1
        BookingStep.COURT -> 2
        BookingStep.REVIEW -> error("Review is rendered separately")
    }
    val title = listOf("选择场馆", "选择时段", "安排场地")[step]
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 44.dp).padding(top = 12.dp, bottom = 110.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("STEP 0${step + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        when (state.configStep) {
            BookingStep.VENUE -> VenueChoices(state, viewModel)
            BookingStep.TIME -> TimeChoices(state, viewModel)
            BookingStep.COURT -> CourtChoices(state, viewModel)
            BookingStep.REVIEW -> Unit
        }
    }
}

@Composable
private fun VenueChoices(state: MainUiState, viewModel: MainViewModel) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Constant.VENUES.forEach { venue ->
            val selected = state.selectedVenue == venue
            Surface(
                onClick = { viewModel.onVenueSelected(venue) },
                modifier = Modifier.fillMaxWidth().height(101.dp),
                shape = RoundedCornerShape(22.dp),
                color = if (selected) colors.secondaryContainer else colors.surface,
                border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(17.dp)
                ) {
                    Box(
                        modifier = Modifier.size(50.dp).background(
                            if (selected) colors.primary else colors.surfaceContainerHigh,
                            RoundedCornerShape(16.dp)
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            venueSymbol(venue),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (selected) colors.onPrimary else colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(venue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "${Constant.VENUE_COURT_COUNTS[venue]} 片场地",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    if (selected) Icon(Icons.Rounded.Check, contentDescription = "已选择", tint = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun TimeChoices(state: MainUiState, viewModel: MainViewModel) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Constant.TIME_SLOTS.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { slot ->
                    val selected = state.selectedTimeSlot == slot
                    Surface(
                        onClick = { viewModel.onAction(MainAction.SelectTimeSlot(slot)) },
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(19.dp),
                        color = if (selected) colors.secondaryContainer else colors.surface,
                        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                compactTimeSlot(slot),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            if (selected) Icon(Icons.Rounded.Check, contentDescription = "已选择", tint = colors.primary, modifier = Modifier.size(19.dp))
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CourtChoices(state: MainUiState, viewModel: MainViewModel) {
    val colors = MaterialTheme.colorScheme
    val columns = if (state.courtCount <= 9) 5 else 6
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "按喜好点选，顺序即优先级",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = { viewModel.onAction(MainAction.ClearSelectedCourts) },
            enabled = state.selectedCourts.isNotEmpty()
        ) {
            Text("清空")
        }
    }
    Spacer(Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        (1..state.courtCount).chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { court ->
                    val rank = state.selectedCourts.indexOf(court)
                    val selected = rank >= 0
                    Surface(
                        onClick = { viewModel.onCourtClicked(court) },
                        modifier = Modifier.weight(1f).height(61.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) colors.primary else colors.surfaceContainerLow,
                        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                court.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) colors.onPrimary else colors.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            if (selected) Text(
                                (rank + 1).toString(),
                                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onPrimary,
                            )
                        }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    HorizontalDivider(color = colors.outlineVariant)
    Spacer(Modifier.height(12.dp))
    Text(
        "优先顺序",
        style = MaterialTheme.typography.labelMedium,
        color = colors.onSurfaceVariant,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(10.dp))
    if (state.selectedCourts.isEmpty()) {
        Text(
            "未选择 · 全部场地随机尝试",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
    } else {
        state.selectedCourts.chunked(8).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(bottom = 7.dp)) {
                row.forEach { court ->
                    val rank = state.selectedCourts.indexOf(court) + 1
                    Surface(
                        onClick = { viewModel.onCourtClicked(court) },
                        shape = CircleShape,
                        color = colors.secondaryContainer
                    ) {
                        Text(
                            "${rank.toString().padStart(2, '0')}  ${court}号 ×",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewScreen(state: MainUiState) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 44.dp).padding(top = 12.dp, bottom = 110.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(state.selectedVenue, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                    Text(
                        compactTimeSlot(state.selectedTimeSlot),
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier.size(130.dp).background(colors.secondaryContainer, RoundedCornerShape(38.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        venueSymbol(state.selectedVenue),
                        style = MaterialTheme.typography.displayLarge,
                        color = colors.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(38.dp))
            HorizontalDivider(color = colors.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 23.dp),
                horizontalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                ReviewFact(Modifier.weight(1f), "目标日期", formatBookingDate(state.targetDate))
                ReviewFact(Modifier.weight(1f), "执行时间", state.executionTime)
            }
            HorizontalDivider(color = colors.outlineVariant)
            Spacer(Modifier.height(27.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Place, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("优先顺序", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    if (state.selectedCourts.isEmpty()) "随机尝试" else "${state.selectedCourts.size} 片已选",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            if (state.selectedCourts.isEmpty()) {
                Surface(shape = RoundedCornerShape(12.dp), color = colors.secondaryContainer) {
                    Text("全部场地随机尝试", modifier = Modifier.padding(12.dp), color = colors.onSecondaryContainer)
                }
            } else {
                state.selectedCourts.chunked(12).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(bottom = 7.dp)) {
                        row.forEach { court ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.secondaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(court.toString(), color = colors.onSecondaryContainer, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "其余 ${state.courtCount - state.selectedCourts.size} 片场地随机接在队尾",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

private fun venueSymbol(venue: String): String = venue.firstOrNull()?.toString().orEmpty()

@Composable
private fun ReviewFact(modifier: Modifier, label: String, value: String) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
