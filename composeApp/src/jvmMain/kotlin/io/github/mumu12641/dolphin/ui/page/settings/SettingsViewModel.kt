package io.github.mumu12641.dolphin.ui.page.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.darkrockstudios.libraries.mpfilepicker.MPFile
import io.github.mumu12641.dolphin.data.PreferencesRepository
import io.github.mumu12641.dolphin.di.DatabaseModule
import io.github.mumu12641.dolphin.model.HistoryEntry
import io.github.mumu12641.dolphin.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

data class SettingsUiState(
    val username: String = "",
    val password: String = "",
    val history: List<HistoryEntry> = emptyList(),
    val color: Color = Color(0xFF89CFF0),
    val darkMode: Boolean = false,
    val executionTime: String = "08:00:03",
    val timeDirty: Boolean = false,
    val accountOpen: Boolean = false,
    val colorPickerOpen: Boolean = false,
    val showFilePicker: Boolean = false,
    val importError: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel : ViewModel() {
    private val historyRepository = DatabaseModule.historyRepository
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()
    private val timePattern = Regex("""([01]\d|2[0-3]):[0-5]\d:[0-5]\d""")
    private var persistedUser = "" to ""

    init {
        viewModelScope.launch {
            PreferencesRepository.userFlow.flatMapLatest { (username, password) ->
                historyRepository.getHistoryByUsername(username).map {
                    Triple(username, password, it?.history ?: emptyList())
                }
            }.collect { (username, password, history) ->
                persistedUser = username to password
                _uiState.update {
                    it.copy(
                        username = if (it.accountOpen) it.username else username,
                        password = if (it.accountOpen) it.password else password,
                        history = history
                    )
                }
            }
        }
        viewModelScope.launch {
            PreferencesRepository.themeFlow.collect { (color, dark) ->
                _uiState.update { it.copy(color = color.toComposeColor(), darkMode = dark.toBoolean()) }
            }
        }
        viewModelScope.launch {
            PreferencesRepository.executionTimeFlow.collect { time ->
                _uiState.update {
                    if (it.timeDirty) it else it.copy(executionTime = time)
                }
            }
        }
    }

    fun openAccount() = _uiState.update { it.copy(accountOpen = true, importError = null) }
    fun closeAccount() = _uiState.update {
        it.copy(
            accountOpen = false,
            username = persistedUser.first,
            password = persistedUser.second,
            importError = null
        )
    }
    fun updateUsername(value: String) = _uiState.update { it.copy(username = value) }
    fun updatePassword(value: String) = _uiState.update { it.copy(password = value) }
    fun openFilePicker() = _uiState.update { it.copy(showFilePicker = true) }
    fun openColorPicker() = _uiState.update { it.copy(colorPickerOpen = true) }
    fun closeColorPicker() = _uiState.update { it.copy(colorPickerOpen = false) }
    fun updateColor(color: Color) = _uiState.update { it.copy(color = color) }
    fun updateExecutionTime(value: String) = _uiState.update {
        it.copy(executionTime = value, timeDirty = true)
    }

    fun saveUser() {
        val (username, password) = _uiState.value
        persistedUser = username.trim() to password
        viewModelScope.launch { PreferencesRepository.saveUser(username.trim(), password) }
        closeAccount()
    }

    fun importUser(file: MPFile<Any>?) {
        _uiState.update { it.copy(showFilePicker = false) }
        file ?: return
        viewModelScope.launch {
            try {
                val user = Json.decodeFromString<User>(file.getFileByteArray().decodeToString())
                _uiState.update { it.copy(username = user.user, password = user.pwd, importError = null) }
            } catch (error: Exception) {
                _uiState.update { it.copy(importError = "无法读取账号文件：${error.message}") }
            }
        }
    }

    fun selectPreset(color: Color) {
        _uiState.update { it.copy(color = color) }
        saveTheme()
    }

    fun setDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(darkMode = enabled) }
        saveTheme()
    }

    fun saveTheme() {
        val current = _uiState.value
        val hex = String.format("#%06X", current.color.toArgb() and 0xFFFFFF)
        viewModelScope.launch {
            PreferencesRepository.saveTheme(hex, current.darkMode.toString())
        }
    }

    fun saveExecutionTime() {
        val time = _uiState.value.executionTime
        if (!timePattern.matches(time)) return
        _uiState.update { it.copy(timeDirty = false) }
        viewModelScope.launch { PreferencesRepository.saveExecutionTime(time) }
    }

    fun isExecutionTimeValid() = timePattern.matches(_uiState.value.executionTime)
}

fun String.toComposeColor(): Color {
    val hex = removePrefix("#").removePrefix("0x").removePrefix("0X")
    return try {
        when (hex.length) {
            6 -> Color(hex.toLong(16).toInt() or 0xFF000000.toInt())
            8 -> Color(hex.toLong(16).toInt())
            else -> Color(0xFF89CFF0)
        }
    } catch (_: NumberFormatException) {
        Color(0xFF89CFF0)
    }
}
