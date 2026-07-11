package ch.rhosys.gitzi.ui.epics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.gitzi.ui.common.EmptyState
import ch.rhosys.gitzi.ui.common.PriorityBadge
import ch.rhosys.gitzi.ui.common.StageChip

@Composable
fun EpicDetailScreen(
    onTaskClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: EpicDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.epic?.title ?: "Epic") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New task")
            }
        },
    ) { padding ->
        if (state.tasks.isEmpty() && !state.isLoading) {
            EmptyState("No tasks in this epic yet.", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.epic?.description?.let { description ->
                    item { Text(description, style = MaterialTheme.typography.bodyMedium) }
                }
                items(state.tasks, key = { it.id }) { task ->
                    Card(onClick = { onTaskClick(task.id) }, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(task.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                StageChip(task.stage.toColumn())
                                PriorityBadge(task.priority)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, description, priority ->
                viewModel.createTask(title, description, priority)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun CreateTaskDialog(onDismiss: () -> Unit, onCreate: (String, String?, Int?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it.filter(Char::isDigit) },
                    label = { Text("Priority (lower = more urgent)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title, description.ifBlank { null }, priority.toIntOrNull()) },
                enabled = title.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
