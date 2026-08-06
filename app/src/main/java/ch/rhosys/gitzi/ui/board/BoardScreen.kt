package ch.rhosys.gitzi.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column as ColumnLayout
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.gitzi.domain.model.Column
import ch.rhosys.gitzi.domain.model.Task
import ch.rhosys.gitzi.ui.common.PriorityBadge
import ch.rhosys.gitzi.ui.common.color
import ch.rhosys.gitzi.ui.theme.StageBuffer

@Composable
fun BoardScreen(onTaskClick: (String) -> Unit, viewModel: BoardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column.laneColumns().forEach { column ->
            val tasks = state.tasksByColumn[column].orEmpty()
            item(key = column.name) {
                BoardSection(column = column, tasks = tasks, onTaskClick = onTaskClick)
            }
        }
    }
}

@Composable
private fun BoardSection(column: Column, tasks: List<Task>, onTaskClick: (String) -> Unit) {
    ColumnLayout(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(column.color().copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = "${column.label} (${tasks.size})",
                style = MaterialTheme.typography.titleSmall,
                color = column.color(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ColumnLayout(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tasks.forEach { task ->
                val isBuffered = task.stage.toColumn().isBuffer
                TaskCard(task = task, isBuffered = isBuffered, onClick = { onTaskClick(task.id) })
            }
        }
    }
}

@Composable
fun TaskCard(task: Task, isBuffered: Boolean = false, onClick: () -> Unit) {
    val containerColor = if (isBuffered) {
        StageBuffer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        ColumnLayout(modifier = Modifier.padding(12.dp)) {
            if (isBuffered) {
                Text(
                    text = "Awaiting review",
                    style = MaterialTheme.typography.labelSmall,
                    color = StageBuffer,
                )
            }
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Box(modifier = Modifier.padding(top = 8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PriorityBadge(task.priority)
                }
            }
            task.agent?.let {
                Text(
                    text = "agent: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (task.blockedBy.isNotEmpty()) {
                Text(
                    text = "blocked by ${task.blockedBy.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
