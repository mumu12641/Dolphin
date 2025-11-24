package io.github.mumu12641.dolphin.ui.page.settings

import androidx.compose.ui.graphics.Color
import com.darkrockstudios.libraries.mpfilepicker.MPFile
import io.github.mohammedalaamorsi.colorpicker.ext.toHex
import io.github.mumu12641.dolphin.data.UserPreferencesRepository
import io.github.mumu12641.dolphin.di.Graph
import io.github.mumu12641.dolphin.model.HistoryEntry
import io.github.mumu12641.dolphin.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

sealed class SettingsAction {
    data class UpdateUsername(val username: String) : SettingsAction()
    data class UpdatePassword(val password: String) : SettingsAction()
    data class SelectFile(val file: MPFile<Any>?) : SettingsAction()
    data class SelectColor(val color: Color) : SettingsAction()
    data object OpenFilePicker : SettingsAction()
    data object SaveUser : SettingsAction()
    data object SaveColor : SettingsAction()
    data object OpenAccountDialog : SettingsAction()
    data object DismissAccountDialog : SettingsAction()
    data object OpenThemeDialog : SettingsAction()
    data object DismissThemeDialog : SettingsAction()
}

data class SettingsUiState(
    val username: String = "",
    val password: String = "",
    val history: List<HistoryEntry> = emptyList(),
    val showFilePicker: Boolean = false,
    val showAccountDialog: Boolean = false,
    val showThemeDialog: Boolean = false,
    val color: Color = Color.White
)

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel : ViewModel() {

    private val historyRepository = Graph.historyRepository

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    //    init {
//        viewModelScope.launch {
//            UserPreferencesRepository.userFlow
//                .flatMapLatest { user ->
//                    val username = user.first
//                    val password = user.second
//                    if (username.isNotBlank()) {
//                        historyRepository.getHistoryByUsername(username).map {
//                            SettingsUiState(username, password, it?.history ?: emptyList())
//                        }
//                    } else {
//                        flowOf(SettingsUiState(password = password))
//                    }
//                }
//                .collect { state ->
//                    _uiState.value = state
//                }
//        }
//    }
    init {
        viewModelScope.launch {
            val userWithHistoryFlow = UserPreferencesRepository.userFlow
                .flatMapLatest { (username, password) ->
                    historyRepository.getHistoryByUsername(username).map {
                        Triple(username, password, it?.history ?: emptyList())
                    }

                }
            combine(
                userWithHistoryFlow,
                UserPreferencesRepository.colorFlow
            ) { (username, password, history), colorString ->
                SettingsUiState(
                    username = username,
                    password = password,
                    history = history,
                    color = colorString.toComposeColor()
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.UpdateUsername -> _uiState.update { it.copy(username = action.username) }
            is SettingsAction.UpdatePassword -> _uiState.update { it.copy(password = action.password) }
            is SettingsAction.SelectFile -> onFileSelected(action.file)
            is SettingsAction.SelectColor -> _uiState.update { it.copy(color = action.color) }
            SettingsAction.OpenFilePicker -> _uiState.update { it.copy(showFilePicker = true) }
            SettingsAction.SaveUser -> saveUser()
            SettingsAction.SaveColor -> saveColor()
            SettingsAction.OpenAccountDialog -> _uiState.update { it.copy(showAccountDialog = true) }
            SettingsAction.DismissAccountDialog -> _uiState.update { it.copy(showAccountDialog = false) }
            SettingsAction.OpenThemeDialog -> _uiState.update { it.copy(showThemeDialog = true) }
            SettingsAction.DismissThemeDialog -> _uiState.update { it.copy(showThemeDialog = false) }
        }
    }

    private fun onFileSelected(file: MPFile<Any>?) {
        _uiState.update { it.copy(showFilePicker = false) }
        file ?: return

        viewModelScope.launch {
            val content = file.getFileByteArray().decodeToString()
            importUserFromJson(content)
        }
    }

    private fun importUserFromJson(jsonContent: String) {
        try {
            val user = Json.decodeFromString<User>(jsonContent)
            _uiState.update { it.copy(username = user.user, password = user.pwd) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveUser() {
        viewModelScope.launch {
            UserPreferencesRepository.saveUser(_uiState.value.username, _uiState.value.password)
        }
    }

    private fun saveColor() {
        viewModelScope.launch {
            UserPreferencesRepository.saveColor(_uiState.value.color.toHex())
        }
    }

}

fun String.toComposeColor(): Color {
    val hex = this.removePrefix("#")
    return try {
        when (hex.length) {
            6 -> {
                val red = hex.take(2).toInt(16)
                val green = hex.substring(2, 4).toInt(16)
                val blue = hex.substring(4, 6).toInt(16)
                Color(red, green, blue)
            }

            8 -> {
                val alpha = hex.take(2).toInt(16)
                val red = hex.substring(2, 4).toInt(16)
                val green = hex.substring(4, 6).toInt(16)
                val blue = hex.substring(6, 8).toInt(16)
                Color(red, green, blue, alpha)
            }

            else -> Color.White
        }
    } catch (_: Exception) {
        Color.White
    }
}
