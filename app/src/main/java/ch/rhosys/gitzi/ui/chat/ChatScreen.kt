package ch.rhosys.gitzi.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Chat with the main agent") }) },
        bottomBar = {
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
        },
    ) { padding ->
        if (state.messages.isEmpty()) {
            EmptyState("Say hello — create an epic, ask for status, or just chat.", Modifier.padding(padding))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages) { message -> ChatBubble(message) }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.User
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor =
        when (message.role) {
            ChatRole.User -> MaterialTheme.colorScheme.primaryContainer
            ChatRole.Agent -> MaterialTheme.colorScheme.surfaceVariant
            ChatRole.System -> MaterialTheme.colorScheme.errorContainer
        }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier =
                Modifier
                    .background(bubbleColor, RoundedCornerShape(12.dp))
                    .padding(12.dp),
        ) {
            Text(message.role.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(message.content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
