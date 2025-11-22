package io.github.mumu12641.dolphin.ui.page.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = SettingsViewModel(),
    onNavigateUp: () -> Unit
) {
    FilePicker(show = viewModel.showFilePicker, fileExtensions = listOf("json")) { file ->
        viewModel.onFileSelected(file)
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.openFilePicker()
                    },
                    icon = { Icon(Icons.Default.FileUpload, contentDescription = "导入JSON文件") },
                    text = { Text("导入用户文件") }
                )
                ExtendedFloatingActionButton(
                    onClick = { viewModel.saveUser() },
                    icon = { Icon(Icons.Default.Save, contentDescription = "Save User") },
                    text = { Text("保存用户信息") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                        Icons.Default.Person,
                        contentDescription = "用户信息",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("用户信息", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = viewModel.username,
                        onValueChange = { viewModel.username = it },
                        label = { Text("账号") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewModel.password,
                        onValueChange = { viewModel.password = it },
                        label = { Text("密码") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .background(
//                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
//                        shape = RoundedCornerShape(12.dp)
//                    )
//                    .padding(16.dp)
//            ) {
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Icon(
//                        Icons.Default.ColorLens,
//                        contentDescription = "主题颜色",
//                        tint = MaterialTheme.colorScheme.primary
//                    )
//                    Spacer(Modifier.width(8.dp))
//                    Text("主题颜色", style = MaterialTheme.typography.titleMedium)
//                }
//                Spacer(Modifier.height(16.dp))
//                Row(
//                    Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(16.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Constant.THEME_COLORS.forEach { color ->
//                        Button(
//                            onClick = { themeViewModel.changeThemeColor(color) },
//                            shape = CircleShape,
//                            modifier = Modifier.size(40.dp),
//                            contentPadding = PaddingValues(0.dp)
//                        ) {
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxSize()
//                                    .background(color)
//                            )
//                        }
//                    }
//                }
//            }
        }
    }
}
