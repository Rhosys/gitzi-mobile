package ch.rhosys.gitzi.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.gitzi.domain.model.ReviewItemKind
import ch.rhosys.gitzi.ui.common.EmptyState
import ch.rhosys.gitzi.ui.common.FullScreenLoading
import ch.rhosys.gitzi.ui.common.StageChip

@Composable
fun ReviewScreen(viewModel: ReviewViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    val current = state.current
    when {
        state.isLoading -> FullScreenLoading()
        current == null -> EmptyState("Nothing needs your attention right now.")
        else ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.relatedTask?.let { task ->
                    Text(task.title, style = MaterialTheme.typography.titleMedium)
                    StageChip(task.stage.toColumn())
                }

                when (val kind = current.kind) {
                    is ReviewItemKind.AgentQuestion ->
                        QuestionCard(
                            question = kind.question,
                            isSubmitting = state.isSubmitting,
                            onAnswer = { answer -> viewModel.answer(current.id, answer) },
                        )
                    is ReviewItemKind.BufferApproval ->
                        ApprovalCard(
                            agentOutput = state.relatedTask?.agentOutput,
                            isSubmitting = state.isSubmitting,
                            onApprove = { viewModel.approve(current.id) },
                            onReject = { feedback -> viewModel.reject(current.id, feedback) },
                        )
                }

                if (state.remainingCount > 0) {
                    Text(
                        "${state.remainingCount} more waiting",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
    }
}

@Composable
private fun QuestionCard(question: String, isSubmitting: Boolean, onAnswer: (String) -> Unit) {
    var answer by remember { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("The agent is blocked and needs an answer:", style = MaterialTheme.typography.labelMedium)
            Text(question, style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text("Your answer") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onAnswer(answer) },
                enabled = !isSubmitting && answer.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSubmitting) CircularProgressIndicator(Modifier.padding(2.dp)) else Text("Send answer")
            }
        }
    }
}

@Composable
private fun ApprovalCard(
    agentOutput: String?,
    isSubmitting: Boolean,
    onApprove: () -> Unit,
    onReject: (String) -> Unit,
) {
    var showRejectField by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ready to advance — review the agent's output:", style = MaterialTheme.typography.labelMedium)
            Text(agentOutput ?: "(no output recorded)", style = MaterialTheme.typography.bodyMedium)

            if (showRejectField) {
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    label = { Text("What needs to change?") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { onReject(feedback) },
                    enabled = !isSubmitting && feedback.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Send rejection") }
            } else {
                Button(onClick = onApprove, enabled = !isSubmitting, modifier = Modifier.fillMaxWidth()) {
                    if (isSubmitting) CircularProgressIndicator(Modifier.padding(2.dp)) else Text("Approve")
                }
                OutlinedButton(
                    onClick = { showRejectField = true },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Reject") }
            }
        }
    }
}
