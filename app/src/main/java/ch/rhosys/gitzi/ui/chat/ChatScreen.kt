package ch.rhosys.gitzi.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.gitzi.domain.model.ChatMessage
import ch.rhosys.gitzi.domain.model.ChatRole
import ch.rhosys.gitzi.ui.common.EmptyState
import kotlinx.coroutines.delay

@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(
        modifier = Modifier.fillMaxSize().imePadding(),
    ) {
        if (state.messages.isEmpty()) {
            EmptyState(
                "Say hello — create an epic, ask for status, or just chat.",
                Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        isEditing = state.editingMessageId == message.id,
                        isCopyMode = state.copyModeMessageId == message.id,
                        onStartEdit = { viewModel.startEditing(message.id) },
                        onCancelEdit = { viewModel.cancelEditing() },
                        onSubmitEdit = { newContent -> viewModel.submitEdit(message, newContent) },
                        onRequestDelete = { viewModel.requestDelete(message) },
                        onEnterCopyMode = { viewModel.enterCopyMode(message.id) },
                        onExitCopyMode = { viewModel.exitCopyMode() },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message the main agent…") },
            )
            IconButton(
                onClick = {
                    viewModel.sendMessage(draft)
                    draft = ""
                },
                enabled = !state.isSending && draft.isNotBlank(),
            ) { Icon(Icons.Default.Send, contentDescription = "Send") }
        }
    }

    state.deletingMessage?.let {
        DeleteMessageDialog(
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.cancelDelete() },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
    message: ChatMessage,
    isEditing: Boolean,
    isCopyMode: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSubmitEdit: (String) -> Unit,
    onRequestDelete: () -> Unit,
    onEnterCopyMode: () -> Unit,
    onExitCopyMode: () -> Unit,
) {
    val isUser = message.role == ChatRole.User
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor =
        when (message.role) {
            ChatRole.User -> MaterialTheme.colorScheme.primaryContainer
            ChatRole.Agent -> MaterialTheme.colorScheme.surfaceVariant
            ChatRole.System -> MaterialTheme.colorScheme.errorContainer
        }

    var showContextMenu by remember { mutableStateOf(false) }

    if (isCopyMode) {
        LaunchedEffect(Unit) {
            delay(20_000)
            onExitCopyMode()
        }
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        if (isEditing) {
            EditBubble(
                initialContent = message.content,
                bubbleColor = bubbleColor,
                onCancel = onCancelEdit,
                onSubmit = onSubmitEdit,
            )
        } else {
            Column(
                modifier =
                    Modifier
                        .combinedClickable(
                            onClick = { if (isCopyMode) onExitCopyMode() },
                            onLongClick = { if (!isCopyMode) showContextMenu = true },
                        )
                        .background(bubbleColor, RoundedCornerShape(12.dp))
                        .padding(12.dp),
            ) {
                Text(
                    message.role.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isCopyMode) {
                    SelectionContainer { Text(message.content, style = MaterialTheme.typography.bodyMedium) }
                } else {
                    Text(message.content, style = MaterialTheme.typography.bodyMedium)
                }
            }

            DropdownMenu(expanded = showContextMenu, onDismissRequest = { showContextMenu = false }) {
                DropdownMenuItem(text = { Text("Copy") }, onClick = {
                    showContextMenu = false
                    onEnterCopyMode()
                })
                if (isUser) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = {
                        showContextMenu = false
                        onStartEdit()
                    })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = {
                        showContextMenu = false
                        onRequestDelete()
                    })
                }
            }
        }
    }
}

@Composable
private fun EditBubble(
    initialContent: String,
    bubbleColor: androidx.compose.ui.graphics.Color,
    onCancel: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var editText by remember(initialContent) { mutableStateOf(initialContent) }

    Column(
        modifier =
            Modifier
                .background(bubbleColor, RoundedCornerShape(12.dp))
                .padding(12.dp),
    ) {
        OutlinedTextField(
            value = editText,
            onValueChange = { editText = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            TextButton(
                onClick = { onSubmit(editText) },
                enabled = editText.isNotBlank() && editText != initialContent,
            ) { Text("Save") }
        }
    }
}

@Composable
private fun DeleteMessageDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete message") },
        text = { Text("Remove this message from the chat session? This cannot be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
