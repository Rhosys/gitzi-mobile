package ch.rhosys.gitzi.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.rhosys.gitzi.domain.model.ConnectionSettings
import ch.rhosys.gitzi.domain.repository.ConnectionSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private object Keys {
    val SERVER_URL = stringPreferencesKey("server_url")
    val API_TOKEN = stringPreferencesKey("api_token")
    val USE_MOCK_DATA = booleanPreferencesKey("use_mock_data")
    val IS_PAIRED = booleanPreferencesKey("is_paired")
}

class ConnectionSettingsDataStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ConnectionSettingsRepository {
        override val settings: Flow<ConnectionSettings> =
            dataStore.data.map { prefs ->
                ConnectionSettings(
                    serverUrl = prefs[Keys.SERVER_URL] ?: "",
                    apiToken = prefs[Keys.API_TOKEN] ?: "",
                    useMockData = prefs[Keys.USE_MOCK_DATA] ?: true,
                    isPaired = prefs[Keys.IS_PAIRED] ?: false,
                )
            }

        override suspend fun setServerUrl(url: String) {
            dataStore.edit { it[Keys.SERVER_URL] = url }
        }

        override suspend fun setApiToken(token: String) {
            dataStore.edit { it[Keys.API_TOKEN] = token }
        }

        override suspend fun setUseMockData(enabled: Boolean) {
            dataStore.edit { it[Keys.USE_MOCK_DATA] = enabled }
        }

        override suspend fun setPaired(paired: Boolean) {
            dataStore.edit { it[Keys.IS_PAIRED] = paired }
        }
    }
