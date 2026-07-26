package ch.rhosys.gitzi.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.gitzi.domain.model.AgentDef
import ch.rhosys.gitzi.domain.model.AgentRole
import ch.rhosys.gitzi.domain.model.ProviderDef
import ch.rhosys.gitzi.domain.model.RepoConfig

@Composable
fun SettingsScreen(
    onLogsClick: () -> Unit,
    onDisconnected: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var editingAgent by remember { mutableStateOf<AgentDef?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ConnectionSection(
                state,
                onServerUrlChange = viewModel::setServerUrl,
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
                    state.config.agents.forEach { agent ->
                        AgentRow(agent, onEdit = { editingAgent = agent })
                    }
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
    }

    editingAgent?.let { agent ->
        EditAgentDialog(
            agent = agent,
            providers = state.config.providers.map { it.name },
            onDismiss = { editingAgent = null },
            onSave = { updated ->
                viewModel.updateAgent(updated)
                editingAgent = null
            },
        )
    }
}

@Composable
private fun ConnectionSection(
    state: SettingsUiState,
    onServerUrlChange: (String) -> Unit,
    onDisconnect: () -> Unit,
) {
    SectionCard("Connection") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            var editingUrl by remember(state.connection.serverUrl) {
                mutableStateOf(state.connection.serverUrl)
            }
            OutlinedTextField(
                value = editingUrl,
                onValueChange = { editingUrl = it },
                label = { Text("Server URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (editingUrl != state.connection.serverUrl) {
                Button(
                    onClick = { onServerUrlChange(editingUrl.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save URL") }
            }
            Button(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
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
private fun AgentRow(agent: AgentDef, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(agent.role.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${agent.model}${agent.provider?.let { " · $it" } ?: ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onEdit) { Text("Edit") }
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
private fun EditAgentDialog(
    agent: AgentDef,
    providers: List<String>,
    onDismiss: () -> Unit,
    onSave: (AgentDef) -> Unit,
) {
    var model by remember { mutableStateOf(agent.model) }
    var provider by remember { mutableStateOf(agent.provider ?: "") }
    var providerExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${agent.role.name} agent") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = it },
                ) {
                    OutlinedTextField(
                        value = provider,
                        onValueChange = { provider = it },
                        label = { Text("Provider") },
                        singleLine = true,
                        readOnly = providers.isNotEmpty(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    if (providers.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = providerExpanded,
                            onDismissRequest = { providerExpanded = false },
                        ) {
                            providers.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        provider = name
                                        providerExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(agent.copy(model = model, provider = provider.ifBlank { null }))
                },
                enabled = model.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
