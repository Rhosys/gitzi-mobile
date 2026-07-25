package ch.rhosys.gitzi.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.gitzi.domain.model.AgentDef
import ch.rhosys.gitzi.domain.model.ProviderDef
import ch.rhosys.gitzi.domain.model.RepoConfig

@Composable
fun SettingsScreen(
    onLogsClick: () -> Unit,
    onDisconnected: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ConnectionSection(
                    state,
                    onDisconnect = {
                        viewModel.disconnect()
                        onDisconnected()
                    },
                )
            }

            item {
                SectionCard("Demo mode") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Use mock data", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = state.connection.useMockData, onCheckedChange = viewModel::setUseMockData)
                    }
                }
            }

            item {
                SectionCard("Providers") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.config.providers.forEach { provider -> ProviderRow(provider, onActivate = { viewModel.activateProvider(provider.name) }) }
                        OutlinedButton(
                            onClick = viewModel::discoverProviders,
                            enabled = !state.isDiscovering,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (state.isDiscovering) "Discovering…" else "Discover providers") }
                    }
                }
            }

            item {
                SectionCard("Agent roles") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.config.agents.forEach { agent -> AgentRow(agent) }
                    }
                }
            }

            item {
                SectionCard("Repositories") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (state.config.repos.isEmpty()) {
                            Text("No repos configured.", style = MaterialTheme.typography.bodySmall)
                        }
                        state.config.repos.forEach { repo -> RepoRow(repo) }
                    }
                }
            }

            item {
                SectionCard("Activity") {
                    TextButton(onClick = onLogsClick) { Text("View full activity log") }
                }
            }
        }
    }
}

@Composable
private fun ConnectionSection(state: SettingsUiState, onDisconnect: () -> Unit) {
    SectionCard("Connection") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (state.connection.useMockData) "Demo data (no server)" else state.connection.serverUrl.ifBlank { "Not configured" },
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onDisconnect, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Disconnect")
            }
        }
    }
}

@Composable
private fun ProviderRow(provider: ProviderDef, onActivate: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(provider.name, style = MaterialTheme.typography.bodyMedium)
            val detail = if (provider.apiUrl.isNotBlank()) provider.apiUrl else provider.region ?: provider.kind.name
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (provider.enabled) {
            Text("Active", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        } else {
            TextButton(onClick = onActivate) { Text("Activate") }
        }
    }
}

@Composable
private fun AgentRow(agent: AgentDef) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(agent.role.name, style = MaterialTheme.typography.bodyMedium)
        Text(
            "${agent.model}${agent.provider?.let { " · $it" } ?: ""}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RepoRow(repo: RepoConfig) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(repo.path, style = MaterialTheme.typography.bodyMedium)
        Text(repo.mergeStrategy.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}
