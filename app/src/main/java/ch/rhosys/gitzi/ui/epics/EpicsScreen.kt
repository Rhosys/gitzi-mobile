package ch.rhosys.gitzi.ui.epics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.gitzi.ui.common.EmptyState

@Composable
fun EpicsScreen(onEpicClick: (String) -> Unit, viewModel: EpicsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Epics") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New epic")
            }
        },
    ) { padding ->
        if (state.epics.isEmpty() && !state.isLoading) {
            EmptyState("No epics yet — tap + to create one.", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.epics, key = { it.epic.id }) { summary ->
                    EpicRow(summary, onClick = { onEpicClick(summary.epic.id) })
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateEpicDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, description ->
                viewModel.createEpic(title, description)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun EpicRow(summary: EpicSummary, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(summary.epic.title, style = MaterialTheme.typography.titleMedium)
            summary.epic.description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val progress = if (summary.taskCount == 0) 0f else summary.doneCount.toFloat() / summary.taskCount
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(
                "${summary.doneCount}/${summary.taskCount} tasks done",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun CreateEpicDialog(onDismiss: () -> Unit, onCreate: (String, String?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New epic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(title, description.ifBlank { null }) }, enabled = title.isNotBlank()) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
