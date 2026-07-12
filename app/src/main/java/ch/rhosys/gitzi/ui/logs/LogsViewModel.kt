package ch.rhosys.gitzi.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.gitzi.domain.model.HistoryEntry
import ch.rhosys.gitzi.domain.repository.GitziRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Instant
import javax.inject.Inject

data class LogEntry(val taskTitle: String, val entry: HistoryEntry)

data class LogsUiState(val entries: List<LogEntry> = emptyList(), val isLoading: Boolean = true)

@HiltViewModel
class LogsViewModel
    @Inject
    constructor(repository: GitziRepository) : ViewModel() {
        val uiState: StateFlow<LogsUiState> =
            repository.observeTasks()
                .map { tasks ->
                    val entries =
                        tasks
                            .flatMap { task -> task.history.map { LogEntry(task.title, it) } }
                            .sortedByDescending { logEntryTimestamp(it) }
                    LogsUiState(entries, isLoading = false)
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogsUiState())

        private fun logEntryTimestamp(logEntry: LogEntry): Instant = logEntry.entry.at
    }
