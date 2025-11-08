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
    private val USERNAME_KEY = stringPreferencesKey("username")
    private val PASSWORD_KEY = stringPreferencesKey("password")

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { File(System.getProperty("java.io.tmpdir"), DATASTORE_NAME) }
    )

    val userFlow: Flow<Pair<String, String>> = dataStore.data
        .map { preferences ->
            val username = preferences[USERNAME_KEY] ?: ""
            val password = preferences[PASSWORD_KEY] ?: ""
            Pair(username, password)
        }

    suspend fun saveUser(username: String, password: String) {
        dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
            preferences[PASSWORD_KEY] = password
        }
    }
}
