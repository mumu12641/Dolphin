package io.github.mumu12641.dolphin.ui.page

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
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mumu12641.dolphin.ui.page.main.MainViewModel

@Composable
fun WelcomeScreen(viewModel: MainViewModel, modifier: Modifier, startConfig: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.Top
//        ) {
//            Column {
//                Text(
//                    text = "早安, Xiaoxiao 👋",
//                    style = MaterialTheme.typography.headlineMedium,
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.padding(top = 8.dp)
//                ) {
//                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Text(
//                        text = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 · EEEE")),
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = Color.Gray
//                    )
//                }
//            }
//
//            // Profile Icon
//            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                Box(
//                    modifier = Modifier
//                        .size(48.dp)
//                        .clip(CircleShape)
//                        .background(Brush.linearGradient(listOf(Color(0xFFEADDFF), Color(0xFFD0BCFF))))
//                        .clickable {  },
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(Icons.Rounded.AccountCircle, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
//                }
//            }
//        }

//        Spacer(modifier = Modifier.height(32.dp))

        // Hero Card (Create Task)
        Card(
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clickable {startConfig() },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color(0xFF6750A4), Color(0xFF523E85))))
            ) {
                // Decorative Rocket Icon (Background)
                Icon(
                    imageVector = Icons.Rounded.RocketLaunch,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.1f),
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
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.border(
                                1.dp,
                                Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                        ) {
                            Text(
                                text = "准备就绪",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "创建新任务",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "配置场馆、时间与优先级策略。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
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
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Configs
//        Row(verticalAlignment = Alignment.CenterVertically) {
//            Icon(Icons.Rounded.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
//            Spacer(modifier = Modifier.width(8.dp))
//            Text("最近配置", style = MaterialTheme.typography.titleSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
//        }
//
//        Spacer(modifier = Modifier.height(12.dp))
//
//        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//            // Recent Item 1
//            QuickStartCard(
//                modifier = Modifier.weight(1f),
//                venueName = "西体 · 羽毛球",
//                time = "18:00-20:00",
//                tagColor = SecondaryContainer,
//                tagTextColor = PrimaryColor,
//                onClick = { onQuickStart(TaskData("west", "18:00-20:00", listOf(1, 2, 3, 4))) }
//            )
//            // Recent Item 2
//            QuickStartCard(
//                modifier = Modifier.weight(1f),
//                venueName = "光体 · 羽毛球",
//                time = "20:00-22:00",
//                tagColor = ErrorContainer,
//                tagTextColor = OnErrorContainer,
//                onClick = { onQuickStart(TaskData("light", "20:00-22:00", listOf(10, 11, 12))) }
//            )
//        }
    }
}
