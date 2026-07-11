package ch.rhosys.gitzi.ui.task

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.gitzi.domain.model.HistoryEntry
import ch.rhosys.gitzi.ui.common.EmptyState
import ch.rhosys.gitzi.ui.common.FullScreenLoading
import ch.rhosys.gitzi.ui.common.PriorityBadge
import ch.rhosys.gitzi.ui.common.StageChip

@Composable
fun TaskDetailScreen(onBack: () -> Unit, viewModel: TaskDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showParkDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.task?.title ?: "Task") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        val task = state.task
        when {
            state.isLoading -> FullScreenLoading(Modifier.padding(padding))
            task == null -> EmptyState("Task not found.", Modifier.padding(padding))
            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StageChip(task.stage.toColumn())
                            PriorityBadge(task.priority)
                        }
                    }
                    task.description?.let { description -> item { Text(description, style = MaterialTheme.typography.bodyMedium) } }

                    item {
                        FieldGrid(
                            listOfNotNull(
                                "Agent" to (task.agent ?: "—"),
                                "Branch" to (task.branch ?: "—"),
                                "Repo" to (task.repo ?: "—"),
                            ),
                        )
                    }

                    if (task.blockedBy.isNotEmpty()) {
                        item {
                            LabeledSection("Blocked by") {
                                Text(task.blockedBy.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    task.agentOutput?.let {
                        item { LabeledSection("Agent output") { Text(it, style = MaterialTheme.typography.bodyMedium) } }
                    }
                    task.agentFeedback?.let {
                        item {
                            LabeledSection("Feedback") {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    task.resumeSummary?.let {
                        item { LabeledSection("Resume summary") { Text(it, style = MaterialTheme.typography.bodyMedium) } }
                    }

                    item {
                        TextButton(onClick = { showParkDialog = true }) { Text("Park this task") }
                    }

                    if (task.history.isNotEmpty()) {
                        item { Text("History", style = MaterialTheme.typography.titleSmall) }
                        items(task.history.reversed()) { entry -> HistoryRow(entry) }
                    }
                }
        }
    }

    if (showParkDialog) {
        ParkTaskDialog(
            onDismiss = { showParkDialog = false },
            onConfirm = { reason ->
                viewModel.parkTask(reason)
                showParkDialog = false
            },
        )
    }
}

@Composable
private fun FieldGrid(fields: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        fields.forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LabeledSection(label: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            content()
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry) {
    val text =
        when (entry) {
            is HistoryEntry.StageChange -> "${entry.from} → ${entry.to}" + (entry.note?.let { " — $it" } ?: "")
            is HistoryEntry.Approval -> "Approved into ${entry.targetStage}"
            is HistoryEntry.Rejection -> "Rejected — ${entry.feedback}"
        }
    Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun ParkTaskDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Park task") },
        text = {
            OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") })
        },
        confirmButton = { TextButton(onClick = { onConfirm(reason) }, enabled = reason.isNotBlank()) { Text("Park") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
