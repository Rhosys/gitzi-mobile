package ch.rhosys.gitzi.ui.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.gitzi.domain.model.Task
import ch.rhosys.gitzi.domain.repository.GitziRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(val task: Task? = null, val isLoading: Boolean = true, val actionMessage: String? = null)

@HiltViewModel
class TaskDetailViewModel
    @Inject
    constructor(
        private val repository: GitziRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val taskId: String = checkNotNull(savedStateHandle["taskId"])

        val uiState: StateFlow<TaskDetailUiState> =
            repository.observeTasks()
                .map { tasks -> TaskDetailUiState(task = tasks.firstOrNull { it.id == taskId }, isLoading = false) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskDetailUiState())

        fun parkTask(reason: String) {
            if (reason.isBlank()) return
            viewModelScope.launch { repository.parkTask(taskId, reason.trim()) }
        }
    }
