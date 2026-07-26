package ch.rhosys.gitzi.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.gitzi.BuildConfig

@Composable
fun SetupScreen(onSetupComplete: () -> Unit, viewModel: SetupViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Connect to Gitzi", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Gitzi runs as a hosted service — this app never talks to a local daemon. " +
                "Point it at your team's deployment, or explore with demo data first.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (BuildConfig.DEBUG) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Use demo data", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Explore every screen with sample epics, tasks, and a review queue — no server needed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.useMockData, onCheckedChange = viewModel::onUseMockDataChange)
                }
                HorizontalDivider(Modifier.padding(top = 16.dp))
            }
        }

        if (!state.useMockData || !BuildConfig.DEBUG) {
            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = viewModel::onServerUrlChange,
                label = { Text("Server URL") },
                placeholder = { Text("https://gitzigo.com/api") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.apiToken,
                onValueChange = viewModel::onApiTokenChange,
                label = { Text("API token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = { viewModel.continueSetup(onSetupComplete) },
            enabled = !state.isTesting && (state.useMockData || state.serverUrl.isNotBlank()),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text(if (state.useMockData) "Continue with demo data" else "Connect")
            }
        }
    }
}
