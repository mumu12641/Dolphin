package io.github.mumu12641.dolphin.ui.page.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import io.github.mohammedalaamorsi.colorpicker.ColorPicker
import io.github.mohammedalaamorsi.colorpicker.ext.toHex
import io.github.mohammedalaamorsi.colorpicker.ext.transparentBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = SettingsViewModel(),
    onNavigateUp: () -> Unit
) {
    val settingUiState by viewModel.uiState.collectAsState()
    val showFilePicker = settingUiState.showFilePicker
    FilePicker(show = showFilePicker, fileExtensions = listOf("json")) { file ->
        viewModel.onAction(SettingsAction.SelectFile(file))
    }

    if (settingUiState.showAccountDialog) {
        AccountDialog(viewModel = viewModel)
    }

    if (settingUiState.showThemeDialog) {
        ThemeDialog(viewModel = viewModel)
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(modifier = Modifier.padding(start = 16.dp)) {
                        Text("设置")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
    ) { paddingValues ->
        SettingContent(modifier = Modifier.padding(paddingValues), viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDialog(viewModel: SettingsViewModel) {
    val settingUiState by viewModel.uiState.collectAsState()

    BasicAlertDialog(
        onDismissRequest = { viewModel.onAction(SettingsAction.DismissAccountDialog) }
    ) {
        Surface(
            modifier = Modifier.width(320.dp).wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "账号信息",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = settingUiState.username,
                    onValueChange = { viewModel.onAction(SettingsAction.UpdateUsername(it)) },
                    label = { Text("账号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Person, contentDescription = null)
                    },
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = settingUiState.password,
                    onValueChange = { viewModel.onAction(SettingsAction.UpdatePassword(it)) },
                    label = { Text("密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
                    },
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { viewModel.onAction(SettingsAction.OpenFilePicker) }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("导入")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            viewModel.onAction(SettingsAction.SaveUser)
                            viewModel.onAction(SettingsAction.DismissAccountDialog)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("确定")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ThemeDialog(viewModel: SettingsViewModel) {
    val settingUiState by viewModel.uiState.collectAsState()
    val color = settingUiState.color

    BasicAlertDialog(
        onDismissRequest = { viewModel.onAction(SettingsAction.DismissThemeDialog) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "选择主题颜色",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ColorPicker {
                        viewModel.onAction(SettingsAction.SelectColor(it))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainer,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "HEX Code",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "#${color.toHex()}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .transparentBackground(verticalBoxesAmount = 6)
                            .background(color)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            MaterialTheme.colorScheme.surfaceContainer,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            viewModel.onAction(SettingsAction.UpdateDarkMode(!settingUiState.darkMode))
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "深色模式",
                            style = MaterialTheme.typography.titleMedium, // 稍微加大字体
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Switch(
                        checked = settingUiState.darkMode,
                        onCheckedChange = { isChecked ->
                            viewModel.onAction(SettingsAction.UpdateDarkMode(isChecked))
                        },
                        modifier = Modifier.scale(0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { viewModel.onAction(SettingsAction.DismissThemeDialog) }
                    ) {
                        Text("取消")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            viewModel.onAction(SettingsAction.SaveTheme)
                            viewModel.onAction(SettingsAction.DismissThemeDialog)
                        }
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingContent(modifier: Modifier, viewModel: SettingsViewModel) {
    val settingUiState by viewModel.uiState.collectAsState()
    val username = settingUiState.username
    val size = settingUiState.history.size
    val successNum = settingUiState.history.count { it.status == 0 }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AccountCircle,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                username,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                Modifier.weight(1f),
                Icons.Rounded.EmojiEvents,
                successNum.toString(),
                "累计成功",
                MaterialTheme.colorScheme.primary
            )
            StatCard(
                Modifier.weight(1f),
                Icons.AutoMirrored.Rounded.ShowChart,
                if (size == 0) "0%" else "${(successNum.toFloat() / size * 100).toInt()}%",
                "成功率",
                MaterialTheme.colorScheme.primary

            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("功能入口", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuCard(
                Modifier.weight(1f),
                Icons.Rounded.VpnKey,
                "账号信息",
                "切换账号",
            ) {
                viewModel.onAction(SettingsAction.OpenAccountDialog)
            }
            MenuCard(
                Modifier.weight(1f),
                Icons.Rounded.Palette,
                "应用主题",
                "更改主题颜色",
            ) {
                viewModel.onAction(SettingsAction.OpenThemeDialog)
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color,
) {
    Card(
        modifier = modifier.clickable { },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text(
                    label,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
fun MenuCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    sub: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 8.dp,
            focusedElevation = 8.dp,
            hoveredElevation = 10.dp
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                sub,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}
