package ch.rhosys.gitzi.data

import ch.rhosys.gitzi.data.mock.MockGitziRepository
import ch.rhosys.gitzi.data.remote.RemoteGitziRepository
import ch.rhosys.gitzi.domain.model.ChatMessage
import ch.rhosys.gitzi.domain.model.Epic
import ch.rhosys.gitzi.domain.model.GitziConfig
import ch.rhosys.gitzi.domain.model.HumanReviewItem
import ch.rhosys.gitzi.domain.model.ProviderDef
import ch.rhosys.gitzi.domain.model.Task
import ch.rhosys.gitzi.domain.repository.ConnectionSettingsRepository
import ch.rhosys.gitzi.domain.repository.GitziRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug-only hot-swap between the in-memory demo backend and the real
 * (assumed) Gitzi API — flip "Use mock data" in Settings and every screen
 * re-points instantly, no rebuild or restart. Release builds skip this
 * entirely and bind [RemoteGitziRepository] directly (see the `release`
 * source set's RepositoryModule) so production never silently shows fake data.
 */
@Singleton
class SwitchableGitziRepository
    @Inject
    constructor(
        private val mock: MockGitziRepository,
        private val remote: RemoteGitziRepository,
        private val connectionSettings: ConnectionSettingsRepository,
    ) : GitziRepository {
        private val activeBackend: Flow<GitziRepository> =
            connectionSettings.settings
                .map { it.useMockData }
                .distinctUntilChanged()
                .map { useMock -> if (useMock) mock else remote }

        private fun <T> observeFromActive(select: (GitziRepository) -> Flow<T>): Flow<T> =
            activeBackend.flatMapLatest(select)

        override fun observeEpics(): Flow<List<Epic>> = observeFromActive { it.observeEpics() }

        override fun observeTasks(): Flow<List<Task>> = observeFromActive { it.observeTasks() }

        override fun observeReviewQueue(): Flow<List<HumanReviewItem>> = observeFromActive { it.observeReviewQueue() }

        override fun observeChat(): Flow<List<ChatMessage>> = observeFromActive { it.observeChat() }

        override fun observeConfig(): Flow<GitziConfig> = observeFromActive { it.observeConfig() }

        private suspend fun active(): GitziRepository = if (connectionSettings.settings.first().useMockData) mock else remote

        override suspend fun refreshAll() = active().refreshAll()

        override suspend fun testConnection() = active().testConnection()

        override suspend fun createEpic(title: String, description: String?) = active().createEpic(title, description)

        override suspend fun createTask(epicId: String, title: String, description: String?, priority: Int?, repo: String?) =
            active().createTask(epicId, title, description, priority, repo)

        override suspend fun updateTask(taskId: String, title: String?, description: String?) =
            active().updateTask(taskId, title, description)

        override suspend fun parkTask(taskId: String, reason: String) = active().parkTask(taskId, reason)

        override suspend fun blockTask(taskId: String, blockedBy: List<String>) = active().blockTask(taskId, blockedBy)

        override suspend fun answerReviewItem(itemId: String, content: String) = active().answerReviewItem(itemId, content)

        override suspend fun approveReviewItem(itemId: String) = active().approveReviewItem(itemId)

        override suspend fun rejectReviewItem(itemId: String, feedback: String) = active().rejectReviewItem(itemId, feedback)

        override suspend fun sendChatMessage(content: String) = active().sendChatMessage(content)

        override suspend fun updateConfig(config: GitziConfig) = active().updateConfig(config)

        override suspend fun discoverProviders(): Result<List<ProviderDef>> = active().discoverProviders()

        override suspend fun activateProvider(name: String) = active().activateProvider(name)
    }
