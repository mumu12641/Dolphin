package io.github.mumu12641.dolphin.ui.page.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.darkrockstudios.libraries.mpfilepicker.MPFile
import io.github.mumu12641.dolphin.data.UserPreferencesRepository
import io.github.mumu12641.dolphin.di.Graph
import io.github.mumu12641.dolphin.model.HistoryEntry
import io.github.mumu12641.dolphin.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

sealed class SettingsAction {
    data class UpdateUsername(val username: String) : SettingsAction()
    data class UpdatePassword(val password: String) : SettingsAction()
    data class SelectFile(val file: MPFile<Any>?) : SettingsAction()
    data object OpenFilePicker : SettingsAction()
    data object SaveUser : SettingsAction()
    data object OpenAccountDialog : SettingsAction()
    data object DismissAccountDialog : SettingsAction()
}

data class SettingsUiState(
    val username: String = "",
    val password: String = "",
    val history: List<HistoryEntry> = emptyList(),
    val showFilePicker: Boolean = false,
    val showAccountDialog: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel : ViewModel() {

    private val historyRepository = Graph.historyRepository

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            UserPreferencesRepository.userFlow
                .flatMapLatest { user ->
                    val username = user.first
                    val password = user.second
                    if (username.isNotBlank()) {
                        historyRepository.getHistoryByUsername(username).map {
                            SettingsUiState(username, password, it?.history ?: emptyList())
                        }
                    } else {
                        flowOf(SettingsUiState(password = password))
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.UpdateUsername -> _uiState.update { it.copy(username = action.username) }
            is SettingsAction.UpdatePassword -> _uiState.update { it.copy(password = action.password) }
            is SettingsAction.SelectFile -> onFileSelected(action.file)
            SettingsAction.OpenFilePicker -> _uiState.update { it.copy(showFilePicker = true) }
            SettingsAction.SaveUser -> saveUser()
            SettingsAction.OpenAccountDialog -> _uiState.update { it.copy(showAccountDialog = true) }
            SettingsAction.DismissAccountDialog -> _uiState.update { it.copy(showAccountDialog = false) }
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
}
