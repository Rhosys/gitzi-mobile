package ch.rhosys.gitzi.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.gitzi.domain.repository.ConnectionSettingsRepository
import ch.rhosys.gitzi.domain.repository.GitziRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val serverUrl: String = "",
    val useMockData: Boolean = true,
    val isTesting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SetupViewModel
    @Inject
    constructor(
        private val connectionSettings: ConnectionSettingsRepository,
        private val repository: GitziRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SetupUiState())
        val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val current = connectionSettings.settings.first()
                _uiState.value =
                    SetupUiState(serverUrl = current.serverUrl, useMockData = current.useMockData)
            }
        }

        fun onServerUrlChange(value: String) {
            _uiState.value = _uiState.value.copy(serverUrl = value, error = null)
        }

        fun onUseMockDataChange(value: Boolean) {
            _uiState.value = _uiState.value.copy(useMockData = value)
            viewModelScope.launch { connectionSettings.setUseMockData(value) }
        }

        /** Persists settings, verifies reachability (skipped entirely in mock mode), and marks the device paired. */
        fun continueSetup(onDone: () -> Unit) {
            val state = _uiState.value
            viewModelScope.launch {
                connectionSettings.setServerUrl(state.serverUrl.trim())
                connectionSettings.setUseMockData(state.useMockData)

                if (state.useMockData) {
                    connectionSettings.setPaired(true)
                    onDone()
                    return@launch
                }

                _uiState.value = state.copy(isTesting = true, error = null)
                repository.testConnection()
                    .onSuccess {
                        connectionSettings.setPaired(true)
                        _uiState.value = _uiState.value.copy(isTesting = false)
                        onDone()
                    }
                    .onFailure { e ->
                        _uiState.value =
                            _uiState.value.copy(isTesting = false, error = "Couldn't reach that server: ${e.message}")
                    }
            }
        }
    }
