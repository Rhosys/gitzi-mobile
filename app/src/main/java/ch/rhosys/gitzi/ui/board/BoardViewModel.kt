package ch.rhosys.gitzi.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.gitzi.domain.model.Column
import ch.rhosys.gitzi.domain.model.Task
import ch.rhosys.gitzi.domain.repository.GitziRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BoardUiState(
    val tasksByColumn: Map<Column, List<Task>> = emptyMap(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class BoardViewModel
    @Inject
    constructor(private val repository: GitziRepository) : ViewModel() {
        val uiState: StateFlow<BoardUiState> =
            repository.observeTasks()
                .map { tasks ->
                    val grouped = mutableMapOf<Column, MutableList<Task>>()
                    Column.laneColumns().forEach { grouped[it] = mutableListOf() }
                    for (task in tasks) {
                        val column = task.stage.toColumn()
                        val lane = column.parentColumn
                        grouped.getOrPut(lane) { mutableListOf() }.add(task)
                    }
                    BoardUiState(
                        tasksByColumn = grouped.mapValues { (_, v) -> v.sortedBy { it.priority } },
                        isLoading = false,
                    )
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BoardUiState())

        init {
            viewModelScope.launch { repository.refreshAll() }
        }

        fun refresh() {
            viewModelScope.launch { repository.refreshAll() }
        }
    }
