package ch.rhosys.gitzi.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.gitzi.domain.model.AgentDef
import ch.rhosys.gitzi.domain.model.ConnectionSettings
import ch.rhosys.gitzi.domain.model.GitziConfig
import ch.rhosys.gitzi.domain.repository.ConnectionSettingsRepository
import ch.rhosys.gitzi.domain.repository.GitziRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val connection: ConnectionSettings = ConnectionSettings(),
    val config: GitziConfig = GitziConfig(),
    val isDiscovering: Boolean = false,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val repository: GitziRepository,
        private val connectionSettings: ConnectionSettingsRepository,
    ) : ViewModel() {
        private val _isDiscovering = MutableStateFlow(false)

        val uiState: StateFlow<SettingsUiState> =
            combine(connectionSettings.settings, repository.observeConfig(), _isDiscovering) { conn, config, discovering ->
                SettingsUiState(conn, config, discovering)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

        fun setUseMockData(enabled: Boolean) {
            viewModelScope.launch { connectionSettings.setUseMockData(enabled) }
        }

        fun setServerUrl(url: String) {
            viewModelScope.launch { connectionSettings.setServerUrl(url) }
        }

        fun discoverProviders() {
            viewModelScope.launch {
                _isDiscovering.value = true
                repository.discoverProviders()
                _isDiscovering.value = false
            }
        }

        fun activateProvider(name: String) {
            viewModelScope.launch { repository.activateProvider(name) }
        }

        fun updateAgent(updated: AgentDef) {
            viewModelScope.launch {
                val current = uiState.value.config
                val newAgents = current.agents.map { if (it.role == updated.role) updated else it }
                repository.updateConfig(current.copy(agents = newAgents))
            }
        }

        fun disconnect() {
            viewModelScope.launch { connectionSettings.setPaired(false) }
        }
    }
