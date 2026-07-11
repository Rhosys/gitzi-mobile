package ch.rhosys.gitzi.ui.logs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.gitzi.domain.model.HistoryEntry
import ch.rhosys.gitzi.ui.common.EmptyState

@Composable
fun LogsScreen(onBack: () -> Unit, viewModel: LogsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        if (state.entries.isEmpty() && !state.isLoading) {
            EmptyState("No activity yet.", Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(state.entries) { logEntry ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(logEntry.taskTitle, style = MaterialTheme.typography.labelMedium)
                        Text(describe(logEntry.entry), style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun describe(entry: HistoryEntry): String =
    when (entry) {
        is HistoryEntry.StageChange -> "${entry.from} → ${entry.to}" + (entry.note?.let { " — $it" } ?: "")
        is HistoryEntry.Approval -> "Approved into ${entry.targetStage}"
        is HistoryEntry.Rejection -> "Rejected — ${entry.feedback}"
    }
