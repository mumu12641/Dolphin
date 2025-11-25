package io.github.mumu12641.dolphin.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

object PreferencesRepository {

    private const val ACCOUNT_DATASTORE_NAME = ".prefs/account_prefs.preferences_pb"
    private const val THEME_DATASTORE_NAME = ".prefs/theme_prefs.preferences_pb"
    private val USERNAME_KEY = stringPreferencesKey("username")
    private val PASSWORD_KEY = stringPreferencesKey("password")
    private val THEME_COLOR_KEY = stringPreferencesKey("theme_color")
    private val DARK_THEME_KEY = stringPreferencesKey("dark_theme")

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = {
            val file = File(System.getProperty("user.dir"), ACCOUNT_DATASTORE_NAME)
            file.parentFile.mkdirs()
            file
        }
    )
    private val colorDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = {
            val file = File(System.getProperty("user.dir"), THEME_DATASTORE_NAME)
            file.parentFile.mkdirs()
            file
        }
    )

    val userFlow: Flow<Pair<String, String>> = dataStore.data
        .map { preferences ->
            val username = preferences[USERNAME_KEY] ?: ""
            val password = preferences[PASSWORD_KEY] ?: ""
            Pair(username, password)
        }
    val themeFlow: Flow<Pair<String, String>> = colorDataStore.data
        .map { preferences ->
            val color: String = preferences[THEME_COLOR_KEY] ?: "0x89CFF0"
            val darkTheme: String = preferences[DARK_THEME_KEY] ?: "false"
            Pair(color, darkTheme)
        }

    suspend fun saveUser(username: String, password: String) {
        dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
            preferences[PASSWORD_KEY] = password
        }
    }

    suspend fun saveTheme(color: String, darkTheme: String) {
        colorDataStore.edit { preferences ->
            preferences[THEME_COLOR_KEY] = color
            preferences[DARK_THEME_KEY] = darkTheme
        }
    }
}
