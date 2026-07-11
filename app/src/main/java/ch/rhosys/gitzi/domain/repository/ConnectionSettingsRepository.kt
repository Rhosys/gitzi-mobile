package ch.rhosys.gitzi.domain.repository

import ch.rhosys.gitzi.domain.model.ConnectionSettings
import kotlinx.coroutines.flow.Flow

interface ConnectionSettingsRepository {
    val settings: Flow<ConnectionSettings>

    suspend fun setServerUrl(url: String)

    suspend fun setApiToken(token: String)

    suspend fun setUseMockData(enabled: Boolean)

    suspend fun setPaired(paired: Boolean)
}
