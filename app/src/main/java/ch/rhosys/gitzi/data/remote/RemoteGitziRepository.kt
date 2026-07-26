package ch.rhosys.gitzi.data.remote

import ch.rhosys.gitzi.data.remote.dto.AnswerReviewRequest
import ch.rhosys.gitzi.data.remote.dto.BlockTaskRequest
import ch.rhosys.gitzi.data.remote.dto.CreateEpicRequest
import ch.rhosys.gitzi.data.remote.dto.CreateTaskRequest
import ch.rhosys.gitzi.data.remote.dto.ParkTaskRequest
import ch.rhosys.gitzi.data.remote.dto.RejectReviewRequest
import ch.rhosys.gitzi.data.remote.dto.EditChatMessageRequest
import ch.rhosys.gitzi.data.remote.dto.SendChatRequest
import ch.rhosys.gitzi.data.remote.dto.UpdateConfigRequest
import ch.rhosys.gitzi.data.remote.dto.UpdateTaskRequest
import ch.rhosys.gitzi.data.remote.dto.toDomain
import ch.rhosys.gitzi.data.remote.dto.toDto
import ch.rhosys.gitzi.di.ApplicationScope
import ch.rhosys.gitzi.domain.model.ChatMessage
import ch.rhosys.gitzi.domain.model.Epic
import ch.rhosys.gitzi.domain.model.GitziConfig
import ch.rhosys.gitzi.domain.model.HumanReviewItem
import ch.rhosys.gitzi.domain.model.ProviderDef
import ch.rhosys.gitzi.domain.model.Task
import ch.rhosys.gitzi.domain.repository.ConnectionSettingsRepository
import ch.rhosys.gitzi.domain.repository.GitziRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Talks to the assumed Gitzi HTTP + WebSocket API (docs/api-contract.md).
 * REST calls perform mutations; the WebSocket event stream keeps the
 * observed flows live so every screen reflects what the daemon's dispatcher
 * is doing without polling.
 */
@Singleton
class RemoteGitziRepository
    @Inject
    constructor(
        private val api: GitziApiService,
        private val eventSocket: GitziEventSocket,
        private val connectionSettings: ConnectionSettingsRepository,
        @ApplicationScope private val scope: CoroutineScope,
    ) : GitziRepository {
        private val epics = MutableStateFlow<List<Epic>>(emptyList())
        private val tasks = MutableStateFlow<List<Task>>(emptyList())
        private val reviewQueue = MutableStateFlow<List<HumanReviewItem>>(emptyList())
        private val chat = MutableStateFlow<List<ChatMessage>>(emptyList())
        private val config = MutableStateFlow(GitziConfig())

        private var socketJob: Job? = null

        init {
            connectionSettings.settings
                .map { it.serverUrl to it.apiToken }
                .distinctUntilChanged()
                .onEach { (serverUrl, token) -> reconnectSocket(serverUrl, token) }
                .launchIn(scope)
        }

        private fun reconnectSocket(serverUrl: String, token: String) {
            socketJob?.cancel()
            if (serverUrl.isBlank()) return
            socketJob =
                scope.launch {
                    eventSocket.connect(serverUrl, token).collect { event ->
                        when (event) {
                            is GitziWireEvent.Epics -> epics.value = event.epics.map { it.toDomain() }
                            is GitziWireEvent.Tasks -> tasks.value = event.tasks.map { it.toDomain() }
                            is GitziWireEvent.ReviewQueue -> reviewQueue.value = event.items.map { it.toDomain() }
                            is GitziWireEvent.Chat -> chat.value = event.messages.map { it.toDomain() }
                            is GitziWireEvent.Config -> config.value = event.config.toDomain()
                        }
                    }
                }
        }

        override fun observeEpics(): StateFlow<List<Epic>> = epics

        override fun observeTasks(): StateFlow<List<Task>> = tasks

        override fun observeReviewQueue(): StateFlow<List<HumanReviewItem>> = reviewQueue

        override fun observeChat(): StateFlow<List<ChatMessage>> = chat

        override fun observeConfig(): StateFlow<GitziConfig> = config

        override suspend fun refreshAll(): Result<Unit> =
            runCatching {
                epics.value = api.listEpics().map { it.toDomain() }
                tasks.value = api.listTasks().map { it.toDomain() }
                reviewQueue.value = api.listReviewQueue().map { it.toDomain() }
                chat.value = api.listChat().map { it.toDomain() }
                config.value = api.getConfig().toDomain()
            }

        override suspend fun testConnection(): Result<Unit> = runCatching { api.ping() }

        override suspend fun createEpic(title: String, description: String?): Result<Epic> =
            runCatching { api.createEpic(CreateEpicRequest(title, description)).toDomain() }
                .onSuccess { epics.value = epics.value + it }

        override suspend fun createTask(
            epicId: String,
            title: String,
            description: String?,
            priority: Int?,
            repo: String?,
        ): Result<Task> =
            runCatching {
                api.createTask(CreateTaskRequest(epicId, title, description, priority, repo)).toDomain()
            }.onSuccess { tasks.value = tasks.value + it }

        override suspend fun updateTask(taskId: String, title: String?, description: String?): Result<Task> =
            runCatching { api.updateTask(taskId, UpdateTaskRequest(title, description)).toDomain() }
                .onSuccess { updated -> tasks.value = tasks.value.map { if (it.id == updated.id) updated else it } }

        override suspend fun parkTask(taskId: String, reason: String): Result<Unit> =
            runCatching { api.parkTask(taskId, ParkTaskRequest(reason)) }

        override suspend fun blockTask(taskId: String, blockedBy: List<String>): Result<Unit> =
            runCatching { api.blockTask(taskId, BlockTaskRequest(blockedBy)) }

        override suspend fun answerReviewItem(itemId: String, content: String): Result<Unit> =
            runCatching { api.answerReviewItem(itemId, AnswerReviewRequest(content)) }
                .onSuccess { reviewQueue.value = reviewQueue.value.filterNot { it.id == itemId } }

        override suspend fun approveReviewItem(itemId: String): Result<Unit> =
            runCatching { api.approveReviewItem(itemId) }
                .onSuccess { reviewQueue.value = reviewQueue.value.filterNot { it.id == itemId } }

        override suspend fun rejectReviewItem(itemId: String, feedback: String): Result<Unit> =
            runCatching { api.rejectReviewItem(itemId, RejectReviewRequest(feedback)) }
                .onSuccess { reviewQueue.value = reviewQueue.value.filterNot { it.id == itemId } }

        override suspend fun sendChatMessage(content: String): Result<Unit> =
            runCatching { api.sendChatMessage(SendChatRequest(content)) }

        override suspend fun editChatMessage(sessionId: String, messageId: String, content: String): Result<Unit> =
            runCatching {
                api.editChatMessage(sessionId, messageId, EditChatMessageRequest(content))
                chat.value = chat.value.map { if (it.id == messageId) it.copy(content = content) else it }
            }

        override suspend fun deleteChatMessage(sessionId: String, messageId: String): Result<Unit> =
            runCatching {
                api.deleteChatMessage(sessionId, messageId)
                chat.value = chat.value.filterNot { it.id == messageId }
            }

        override suspend fun updateConfig(config: GitziConfig): Result<Unit> =
            runCatching {
                val updated =
                    api.updateConfig(
                        UpdateConfigRequest(
                            wipLimits = config.wipLimits.overrides,
                            agents = config.agents.map { it.toDto() },
                            repos = config.repos.map { it.toDto() },
                            fallbackProvider = config.fallbackProvider,
                        ),
                    )
                this.config.value = updated.toDomain()
            }

        override suspend fun discoverProviders(): Result<List<ProviderDef>> =
            runCatching { api.discoverProviders().map { it.toDomain() } }

        override suspend fun activateProvider(name: String): Result<Unit> =
            runCatching {
                api.activateProvider(name)
                config.value = api.getConfig().toDomain()
            }
    }
