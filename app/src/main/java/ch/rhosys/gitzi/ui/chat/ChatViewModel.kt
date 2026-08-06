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

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val editingMessageId: String? = null,
    val deletingMessage: ChatMessage? = null,
    val copyModeMessageId: String? = null,
)

@HiltViewModel
class ChatViewModel
    @Inject
    constructor(private val repository: GitziRepository) : ViewModel() {
        private val _isSending = MutableStateFlow(false)
        private val _editingMessageId = MutableStateFlow<String?>(null)
        private val _deletingMessage = MutableStateFlow<ChatMessage?>(null)
        private val _copyModeMessageId = MutableStateFlow<String?>(null)

        val uiState: StateFlow<ChatUiState> =
            combine(
                repository.observeChat(),
                _isSending,
                _editingMessageId,
                _deletingMessage,
                _copyModeMessageId,
            ) { messages, sending, editing, deleting, copyMode ->
                ChatUiState(messages, sending, editing, deleting, copyMode)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

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

        fun startEditing(messageId: String) {
            _editingMessageId.value = messageId
        }

        fun cancelEditing() {
            _editingMessageId.value = null
        }

        fun submitEdit(message: ChatMessage, newContent: String) {
            if (newContent.isBlank()) return
            viewModelScope.launch {
                repository.editChatMessage(message.sessionId, message.id, newContent.trim())
                _editingMessageId.value = null
            }
        }

        fun requestDelete(message: ChatMessage) {
            _deletingMessage.value = message
        }

        fun cancelDelete() {
            _deletingMessage.value = null
        }

        fun confirmDelete() {
            val message = _deletingMessage.value ?: return
            viewModelScope.launch {
                repository.deleteChatMessage(message.sessionId, message.id)
                _deletingMessage.value = null
            }
        }

        fun enterCopyMode(messageId: String) {
            _copyModeMessageId.value = messageId
        }

        fun exitCopyMode() {
            _copyModeMessageId.value = null
        }
    }
