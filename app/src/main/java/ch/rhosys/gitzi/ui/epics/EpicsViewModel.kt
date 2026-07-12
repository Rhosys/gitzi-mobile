package ch.rhosys.gitzi.ui.epics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.gitzi.domain.model.Epic
import ch.rhosys.gitzi.domain.model.Stage
import ch.rhosys.gitzi.domain.model.Task
import ch.rhosys.gitzi.domain.repository.GitziRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EpicSummary(val epic: Epic, val taskCount: Int, val doneCount: Int)

data class EpicsUiState(val epics: List<EpicSummary> = emptyList(), val isLoading: Boolean = true)

@HiltViewModel
class EpicsViewModel
    @Inject
    constructor(private val repository: GitziRepository) : ViewModel() {
        val uiState: StateFlow<EpicsUiState> =
            combine(repository.observeEpics(), repository.observeTasks()) { epics, tasks ->
                EpicsUiState(
                    epics =
                        epics.map { epic ->
                            val epicTasks = tasks.filter { it.epicId == epic.id }
                            EpicSummary(epic, epicTasks.size, epicTasks.count { it.stage == Stage.Done })
                        },
                    isLoading = false,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EpicsUiState())

        private val _isCreatingEpic = MutableStateFlow(false)
        val isCreatingEpic: StateFlow<Boolean> = _isCreatingEpic.asStateFlow()

        init {
            viewModelScope.launch { repository.refreshAll() }
        }

        fun createEpic(title: String, description: String?) {
            if (title.isBlank()) return
            viewModelScope.launch {
                _isCreatingEpic.value = true
                repository.createEpic(title.trim(), description?.trim()?.ifBlank { null })
                _isCreatingEpic.value = false
            }
        }
    }

data class EpicDetailUiState(
    val epic: Epic? = null,
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class EpicDetailViewModel
    @Inject
    constructor(
        private val repository: GitziRepository,
        savedStateHandle: androidx.lifecycle.SavedStateHandle,
    ) : ViewModel() {
        private val epicId: String = checkNotNull(savedStateHandle["epicId"])

        val uiState: StateFlow<EpicDetailUiState> =
            combine(repository.observeEpics(), repository.observeTasks()) { epics, tasks ->
                EpicDetailUiState(
                    epic = epics.firstOrNull { it.id == epicId },
                    tasks = tasks.filter { it.epicId == epicId }.sortedBy { it.priority },
                    isLoading = false,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EpicDetailUiState())

        fun createTask(title: String, description: String?, priority: Int?) {
            if (title.isBlank()) return
            viewModelScope.launch {
                repository.createTask(epicId, title.trim(), description?.trim()?.ifBlank { null }, priority, repo = null)
            }
        }
    }
