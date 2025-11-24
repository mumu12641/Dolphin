package io.github.mumu12641.dolphin.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

object UserPreferencesRepository {

    private const val DATASTORE_NAME = "user_prefs.preferences_pb"
    private const val COLOR_DATASTORE_NAME = "theme_prefs.preferences_pb"
    private val USERNAME_KEY = stringPreferencesKey("username")
    private val PASSWORD_KEY = stringPreferencesKey("password")
    private val COLOR_KEY = stringPreferencesKey("seed_color")

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { File(System.getProperty("java.io.tmpdir"), DATASTORE_NAME) }
    )
    private val colorDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { File(System.getProperty("java.io.tmpdir"), COLOR_DATASTORE_NAME) }
    )

    val userFlow: Flow<Pair<String, String>> = dataStore.data
        .map { preferences ->
            val username = preferences[USERNAME_KEY] ?: ""
            val password = preferences[PASSWORD_KEY] ?: ""
            Pair(username, password)
        }
    val colorFlow: Flow<String> = colorDataStore.data
        .map { preferences ->
            val color: String = preferences[COLOR_KEY] ?: "0xFFFFFFFF"
            color
        }

    suspend fun saveUser(username: String, password: String) {
        dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
            preferences[PASSWORD_KEY] = password
        }
    }

    suspend fun saveColor(color: String) {
        colorDataStore.edit { preferences ->
            preferences[COLOR_KEY] = color
        }
    }
}
