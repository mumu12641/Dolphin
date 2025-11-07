package io.github.mumu12641.dolphin.ui.page.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.darkrockstudios.libraries.mpfilepicker.MPFile
import io.github.mumu12641.dolphin.data.UserPreferencesRepository
import io.github.mumu12641.dolphin.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

class SettingsViewModel : ViewModel() {
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    var showFilePicker by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val user = UserPreferencesRepository.userFlow.first()
            username = user.first
            password = user.second
        }
    }

    fun onFileSelected(file: MPFile<Any>?) {
        showFilePicker = false
        file ?: return

        viewModelScope.launch {
            val content = file.getFileByteArray().decodeToString()
            importUserFromJson(content)
        }
    }

    private fun importUserFromJson(jsonContent: String) {
        println("jsonContent: $jsonContent")
        try {
            val user = Json.decodeFromString<User>(jsonContent)
            username = user.user
            password = user.pwd
        } catch (e: Exception) {
            // Handle JSON parsing error
            e.printStackTrace()
        }
    }

    fun openFilePicker() {
        showFilePicker = true
    }

    fun saveUser() {
        viewModelScope.launch {
            UserPreferencesRepository.saveUser(username, password)
        }
    }
}
