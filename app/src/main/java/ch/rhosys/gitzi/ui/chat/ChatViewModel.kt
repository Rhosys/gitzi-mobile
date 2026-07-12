package ch.rhosys.gitzi.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.gitzi.domain.model.ChatMessage
import ch.rhosys.gitzi.domain.repository.GitziRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(val messages: List<ChatMessage> = emptyList(), val isSending: Boolean = false)

@HiltViewModel
class ChatViewModel
    @Inject
    constructor(private val repository: GitziRepository) : ViewModel() {
        private val _isSending = MutableStateFlow(false)

        val uiState: StateFlow<ChatUiState> =
            combine(repository.observeChat(), _isSending) { messages, sending -> ChatUiState(messages, sending) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

        init {
            viewModelScope.launch { repository.refreshAll() }
        }

        fun sendMessage(content: String) {
            if (content.isBlank()) return
            viewModelScope.launch {
                _isSending.value = true
                repository.sendChatMessage(content.trim())
                _isSending.value = false
            }
        }
    }
