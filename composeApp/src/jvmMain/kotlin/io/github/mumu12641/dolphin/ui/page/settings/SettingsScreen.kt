package io.github.mumu12641.dolphin.ui.page.settings

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import io.github.mohammedalaamorsi.colorpicker.ColorPicker

private val presetColors = listOf(
    Color(0xFF89CFF0),
    Color(0xFF4C6658),
    Color(0xFF445E91),
    Color(0xFF9B5C54),
    Color(0xFF725C8A),
    Color(0xFF138A8A),
    Color(0xFFC68642),
    Color(0xFFD46280)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateUp: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme
    var showTimePicker by remember { mutableStateOf(false) }

    FilePicker(show = state.showFilePicker, fileExtensions = listOf("json")) {
        viewModel.importUser(it)
    }
    if (state.accountOpen) AccountDialog(state, viewModel)
    if (state.colorPickerOpen) ColorDialog(viewModel)
    if (showTimePicker) {
        ExecutionTimeDialog(
            currentTime = state.executionTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { selected ->
                viewModel.updateExecutionTime(selected)
                viewModel.saveExecutionTime()
                showTimePicker = false
            }
        )
    }

    Scaffold(
        containerColor = colors.surface,
        topBar = {
            LargeTopAppBar(
                title = { Text("设置", modifier = Modifier.padding(start = 16.dp)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回上一页")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())
                .padding(horizontal = 44.dp, vertical = 26.dp)
        ) {
            SettingsSection("账号") {
                    Surface(
                        onClick = viewModel::openAccount,
                        modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.primary)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (state.username.isBlank()) "设置账号" else state.username,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "查看或更换预约账号",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            Text(
                                "编辑",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                SettingsSection("主题颜色", "预设色与外观") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetColors.forEachIndexed { index, color ->
                            val selected = state.color.toArgb() == color.toArgb()
                            Surface(
                                onClick = { viewModel.selectPreset(color) },
                                modifier = Modifier.size(38.dp),
                                shape = CircleShape,
                                color = color,
                                border = if (selected) BorderStroke(2.dp, colors.onSurface) else null
                            ) {
                                if (selected) Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = "预设颜色 ${index + 1} 已选",
                                        tint = if (color.luminance() > .5f) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Surface(
                            onClick = viewModel::openColorPicker,
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceContainerLow,
                            border = BorderStroke(1.dp, colors.outline)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Brush,
                                    contentDescription = "自定义主题颜色",
                                    tint = colors.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        listOf(false to "浅色", true to "深色").forEach { (dark, label) ->
                            val selected = state.darkMode == dark
                            Surface(
                                onClick = { viewModel.setDarkMode(dark) },
                                shape = CircleShape,
                                color = if (selected) colors.secondaryContainer else colors.surface,
                                border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant)
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                SettingsSection("执行时间", "本地时间") {
                    Surface(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp)
                        ) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = colors.primary)
                            Text(
                                state.executionTime,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "选择时间",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        }
                    }
                    Text(
                        "到达此时间时执行预约，过于激进会导致封号",
                        modifier = Modifier.padding(top = 9.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
        }
    }
}

@Composable
private fun SettingsSection(title: String, caption: String? = null, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    HorizontalDivider(color = colors.outlineVariant)
    Column(Modifier.fillMaxWidth().padding(vertical = 17.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (caption != null) Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExecutionTimeDialog(
    currentTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = currentTime.split(":")
    val picker = rememberTimePickerState(
        initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 8,
        initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0,
        is24Hour = true
    )
    var second by remember { mutableIntStateOf(parts.getOrNull(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 3) }
    var secondsExpanded by remember { mutableStateOf(false) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(390.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("选择执行时间", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(18.dp))
                TimePicker(state = picker, layoutType = TimePickerLayoutType.Vertical)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("秒", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(14.dp))
                    Box {
                        OutlinedButton(onClick = { secondsExpanded = true }) {
                            Text(second.toString().padStart(2, '0'))
                        }
                        DropdownMenu(
                            expanded = secondsExpanded,
                            onDismissRequest = { secondsExpanded = false }
                        ) {
                            (0..59).forEach { value ->
                                DropdownMenuItem(
                                    text = { Text(value.toString().padStart(2, '0')) },
                                    onClick = {
                                        second = value
                                        secondsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(19.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = {
                        val selected = "%02d:%02d:%02d".format(picker.hour, picker.minute, second)
                        onConfirm(selected)
                    }) { Text("确定") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun AccountDialog(state: SettingsUiState, viewModel: SettingsViewModel) {
    BasicAlertDialog(onDismissRequest = viewModel::closeAccount) {
        Surface(
            modifier = Modifier.width(370.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("账号信息", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = state.username,
                    onValueChange = viewModel::updateUsername,
                    label = { Text("账号") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                state.importError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = viewModel::openFilePicker) { Text("导入 JSON") }
                    TextButton(onClick = viewModel::closeAccount) { Text("取消") }
                    Button(onClick = viewModel::saveUser) { Text("保存") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun ColorDialog(viewModel: SettingsViewModel) {
    BasicAlertDialog(onDismissRequest = {
        viewModel.saveTheme()
        viewModel.closeColorPicker()
    }) {
        Surface(
            modifier = Modifier.width(370.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("自定义主题颜色", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(18.dp))
                ColorPicker { viewModel.updateColor(it) }
                Spacer(Modifier.height(18.dp))
                Button(onClick = {
                    viewModel.saveTheme()
                    viewModel.closeColorPicker()
                }) { Text("完成") }
            }
        }
    }
}
