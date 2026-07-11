package ch.rhosys.gitzi.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.gitzi.domain.model.HumanReviewItem
import ch.rhosys.gitzi.domain.model.Task
import ch.rhosys.gitzi.domain.repository.GitziRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Only ever exposes the single topmost item — [remainingCount] is the most
 * the UI is allowed to say about everything behind it. This mirrors the
 * daemon's `HumanReviewQueue.peek()` and the "one thing at a time" principle
 * in CLAUDE.md: never show the user two things needing attention at once.
 */
data class ReviewUiState(
    val current: HumanReviewItem? = null,
    val relatedTask: Task? = null,
    val remainingCount: Int = 0,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
)

@HiltViewModel
class ReviewViewModel
    @Inject
    constructor(private val repository: GitziRepository) : ViewModel() {
        private val _isSubmitting = MutableStateFlow(false)

        val uiState: StateFlow<ReviewUiState> =
            combine(repository.observeReviewQueue(), repository.observeTasks(), _isSubmitting) { queue, tasks, submitting ->
                val current = queue.firstOrNull()
                ReviewUiState(
                    current = current,
                    relatedTask = tasks.firstOrNull { it.id == current?.taskId },
                    remainingCount = (queue.size - 1).coerceAtLeast(0),
                    isLoading = false,
                    isSubmitting = submitting,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

        init {
            viewModelScope.launch { repository.refreshAll() }
        }

        fun answer(itemId: String, content: String) = submit { repository.answerReviewItem(itemId, content) }

        fun approve(itemId: String) = submit { repository.approveReviewItem(itemId) }

        fun reject(itemId: String, feedback: String) = submit { repository.rejectReviewItem(itemId, feedback) }

        private fun submit(action: suspend () -> Result<Unit>) {
            viewModelScope.launch {
                _isSubmitting.value = true
                action()
                _isSubmitting.value = false
            }
        }
    }
