package ch.rhosys.gitzi.data.mock

import ch.rhosys.gitzi.domain.model.AgentDef
import ch.rhosys.gitzi.domain.model.AgentRole
import ch.rhosys.gitzi.domain.model.ChatMessage
import ch.rhosys.gitzi.domain.model.ChatRole
import ch.rhosys.gitzi.domain.model.Column
import ch.rhosys.gitzi.domain.model.Epic
import ch.rhosys.gitzi.domain.model.GitziConfig
import ch.rhosys.gitzi.domain.model.HistoryEntry
import ch.rhosys.gitzi.domain.model.HumanReviewItem
import ch.rhosys.gitzi.domain.model.MergeStrategy
import ch.rhosys.gitzi.domain.model.ProviderDef
import ch.rhosys.gitzi.domain.model.ProviderKind
import ch.rhosys.gitzi.domain.model.RepoConfig
import ch.rhosys.gitzi.domain.model.ReviewItemKind
import ch.rhosys.gitzi.domain.model.Stage
import ch.rhosys.gitzi.domain.model.Task
import ch.rhosys.gitzi.domain.model.WipLimits
import ch.rhosys.gitzi.domain.repository.GitziRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory stand-in for a deployed Gitzi backend, so the whole app is
 * explorable with no server configured. Debug builds only — see
 * [ch.rhosys.gitzi.data.SwitchableGitziRepository] for how this is swapped
 * with [ch.rhosys.gitzi.data.remote.RemoteGitziRepository] at runtime.
 *
 * Ordering of the review queue replicates `HumanReviewQueue` in the daemon:
 * agent questions always outrank buffer approvals, oldest first; approvals
 * are ordered rightmost-column-first, then by task priority.
 */
@Singleton
class MockGitziRepository
    @Inject
    constructor() : GitziRepository {
        private fun now() = Clock.System.now()

        private val epicAuth = "epic-auth"
        private val epicPush = "epic-push"

        private val seedTasks =
            listOf(
                task("t-oauth-login", epicAuth, "Add OAuth login flow", Stage.Coding, 10, agent = "coder", branch = "gitzi/t-oauth-login-add-oauth"),
                task(
                    "t-token-refresh",
                    epicAuth,
                    "Design token refresh strategy",
                    Stage.ReviewBuffer,
                    20,
                    agentOutput = "## Design: refresh token rotation\n\nRotate refresh tokens on every use, " +
                        "store only a hash server-side, and invalidate the whole chain on reuse detection.",
                ),
                task("t-audit-scopes", epicAuth, "Audit OAuth scopes", Stage.Prioritized, 5),
                task(
                    "t-legacy-cookies",
                    epicAuth,
                    "Remove legacy session cookies",
                    Stage.Prioritized,
                    50,
                    blockedBy = listOf("t-oauth-login"),
                ),
                task("t-push-adapter", epicPush, "Wire APNs/FCM adapter", Stage.Designing, 15, agent = "designer"),
                task(
                    "t-push-ratelimit",
                    epicPush,
                    "Rate-limit push sends",
                    Stage.DeploymentBuffer,
                    8,
                    agentOutput = "Security audit passed — no findings. Ready to deploy.",
                ),
                task("t-push-prefs", epicPush, "Add push preference screen", Stage.Done, 30),
            )

        private val epics =
            MutableStateFlow(
                listOf(
                    Epic(
                        id = epicAuth,
                        title = "Auth overhaul",
                        description = "Migrate to OAuth2 + SSO",
                        taskIds = seedTasks.filter { it.epicId == epicAuth }.map { it.id },
                        createdAt = now(),
                        updatedAt = now(),
                    ),
                    Epic(
                        id = epicPush,
                        title = "Push notification pipeline",
                        description = "Server-driven push for review-queue attention",
                        taskIds = seedTasks.filter { it.epicId == epicPush }.map { it.id },
                        createdAt = now(),
                        updatedAt = now(),
                    ),
                ),
            )

        private val tasks = MutableStateFlow(seedTasks)

        private val reviewQueue =
            MutableStateFlow(
                sortQueue(
                    listOf(
                        HumanReviewItem(
                            id = "rq-1",
                            taskId = "t-push-adapter",
                            kind = ReviewItemKind.AgentQuestion(
                                "Should push tokens be scoped per-device or per-install? The two providers " +
                                    "behave differently across reinstalls.",
                            ),
                            createdAt = now(),
                        ),
                        HumanReviewItem(
                            id = "rq-2",
                            taskId = "t-token-refresh",
                            kind = ReviewItemKind.BufferApproval(Column.ReviewBuffer, 20),
                            createdAt = now(),
                        ),
                        HumanReviewItem(
                            id = "rq-3",
                            taskId = "t-push-ratelimit",
                            kind = ReviewItemKind.BufferApproval(Column.DeploymentBuffer, 8),
                            createdAt = now(),
                        ),
                    ),
                ),
            )

        private val chat =
            MutableStateFlow(
                listOf(
                    ChatMessage(ChatRole.User, "Let's get the auth overhaul epic moving.", now()),
                    ChatMessage(
                        ChatRole.Agent,
                        "Created epic \"Auth overhaul\" with 4 tasks. Coder picked up the OAuth login flow first.",
                        now(),
                    ),
                ),
            )

        private val config =
            MutableStateFlow(
                GitziConfig(
                    wipLimits = WipLimits(),
                    providers =
                        listOf(
                            ProviderDef(
                                name = "lmstudio",
                                kind = ProviderKind.OpenAiCompatible,
                                apiUrl = "http://localhost:1234/v1",
                                enabled = true,
                            ),
                            ProviderDef(
                                name = "bedrock",
                                kind = ProviderKind.Bedrock,
                                region = "us-east-1",
                                modelId = "anthropic.claude-sonnet-4-6-v1:0",
                                enabled = false,
                            ),
                            ProviderDef(
                                name = "ollama-ec2",
                                kind = ProviderKind.OpenAiCompatible,
                                apiUrl = "http://10.0.4.21:11434/v1",
                                enabled = false,
                            ),
                        ),
                    fallbackProvider = "lmstudio",
                    agents = AgentRole.all().map { AgentDef(it, "claude-sonnet-4-6", provider = "lmstudio") },
                    repos = listOf(RepoConfig("workspace/app", MergeStrategy.PullRequest, "main")),
                ),
            )

        override fun observeEpics(): StateFlow<List<Epic>> = epics

        override fun observeTasks(): StateFlow<List<Task>> = tasks

        override fun observeReviewQueue(): StateFlow<List<HumanReviewItem>> = reviewQueue

        override fun observeChat(): StateFlow<List<ChatMessage>> = chat

        override fun observeConfig(): StateFlow<GitziConfig> = config

        override suspend fun refreshAll(): Result<Unit> {
            delay(200)
            return Result.success(Unit)
        }

        override suspend fun testConnection(): Result<Unit> {
            delay(200)
            return Result.success(Unit)
        }

        override suspend fun createEpic(title: String, description: String?): Result<Epic> {
            val epic = Epic(id = "epic-${epics.value.size + 1}", title = title, description = description, createdAt = now(), updatedAt = now())
            epics.value = epics.value + epic
            return Result.success(epic)
        }

        override suspend fun createTask(
            epicId: String,
            title: String,
            description: String?,
            priority: Int?,
            repo: String?,
        ): Result<Task> {
            val newTask =
                Task(
                    id = "t-${tasks.value.size + 1}",
                    epicId = epicId,
                    title = title,
                    description = description,
                    stage = Stage.Prioritized,
                    priority = priority ?: 100,
                    repo = repo,
                    createdAt = now(),
                    updatedAt = now(),
                )
            tasks.value = tasks.value + newTask
            epics.value =
                epics.value.map { if (it.id == epicId) it.copy(taskIds = it.taskIds + newTask.id) else it }
            return Result.success(newTask)
        }

        override suspend fun updateTask(taskId: String, title: String?, description: String?): Result<Task> {
            val existing = tasks.value.firstOrNull { it.id == taskId } ?: return Result.failure(NoSuchElementException(taskId))
            val updated = existing.copy(title = title ?: existing.title, description = description ?: existing.description, updatedAt = now())
            tasks.value = tasks.value.map { if (it.id == taskId) updated else it }
            return Result.success(updated)
        }

        override suspend fun parkTask(taskId: String, reason: String): Result<Unit> {
            mutateTask(taskId) { it.copy(agentFeedback = "Parked: $reason", updatedAt = now()) }
            return Result.success(Unit)
        }

        override suspend fun blockTask(taskId: String, blockedBy: List<String>): Result<Unit> {
            mutateTask(taskId) { it.copy(blockedBy = blockedBy, updatedAt = now()) }
            return Result.success(Unit)
        }

        override suspend fun answerReviewItem(itemId: String, content: String): Result<Unit> {
            val item = reviewQueue.value.firstOrNull { it.id == itemId } ?: return Result.failure(NoSuchElementException(itemId))
            reviewQueue.value = sortQueue(reviewQueue.value.filterNot { it.id == itemId })
            appendChat(ChatMessage(ChatRole.User, content, now()))
            appendChat(ChatMessage(ChatRole.Agent, "Got it — resuming task ${item.taskId}.", now()))
            return Result.success(Unit)
        }

        override suspend fun approveReviewItem(itemId: String): Result<Unit> {
            val item = reviewQueue.value.firstOrNull { it.id == itemId } ?: return Result.failure(NoSuchElementException(itemId))
            reviewQueue.value = sortQueue(reviewQueue.value.filterNot { it.id == itemId })
            advanceTaskPastBuffer(item.taskId)
            return Result.success(Unit)
        }

        override suspend fun rejectReviewItem(itemId: String, feedback: String): Result<Unit> {
            val item = reviewQueue.value.firstOrNull { it.id == itemId } ?: return Result.failure(NoSuchElementException(itemId))
            reviewQueue.value = sortQueue(reviewQueue.value.filterNot { it.id == itemId })
            mutateTask(item.taskId) { task ->
                task.copy(
                    agentFeedback = feedback,
                    history = task.history + HistoryEntry.Rejection(now(), feedback, task.stage),
                    updatedAt = now(),
                )
            }
            return Result.success(Unit)
        }

        override suspend fun sendChatMessage(content: String): Result<Unit> {
            appendChat(ChatMessage(ChatRole.User, content, now()))
            delay(0.3.seconds)
            appendChat(ChatMessage(ChatRole.Agent, "(demo mode — no agent is actually connected)", now()))
            return Result.success(Unit)
        }

        override suspend fun updateConfig(config: GitziConfig): Result<Unit> {
            this.config.value = config
            return Result.success(Unit)
        }

        override suspend fun discoverProviders(): Result<List<ProviderDef>> {
            delay(500)
            return Result.success(config.value.providers)
        }

        override suspend fun activateProvider(name: String): Result<Unit> {
            config.value = config.value.copy(providers = config.value.providers.map { if (it.name == name) it.copy(enabled = true) else it })
            return Result.success(Unit)
        }

        private fun mutateTask(taskId: String, transform: (Task) -> Task) {
            tasks.value = tasks.value.map { if (it.id == taskId) transform(it) else it }
        }

        private fun advanceTaskPastBuffer(taskId: String) {
            mutateTask(taskId) { task ->
                val nextStage =
                    when (task.stage) {
                        Stage.ReviewBuffer -> Stage.Reviewing
                        Stage.CodingBuffer -> Stage.Coding
                        Stage.SecurityAuditBuffer -> Stage.Auditing
                        Stage.DeploymentBuffer -> Stage.Done
                        else -> task.stage
                    }
                task.copy(
                    stage = nextStage,
                    history = task.history + HistoryEntry.Approval(now(), nextStage),
                    updatedAt = now(),
                )
            }
        }

        private fun appendChat(message: ChatMessage) {
            chat.value = chat.value + message
        }

        companion object {
            private fun task(
                id: String,
                epicId: String,
                title: String,
                stage: Stage,
                priority: Int,
                agent: String? = null,
                branch: String? = null,
                agentOutput: String? = null,
                blockedBy: List<String> = emptyList(),
            ): Task {
                val createdAt = Clock.System.now()
                return Task(
                    id = id,
                    epicId = epicId,
                    title = title,
                    stage = stage,
                    priority = priority,
                    agent = agent,
                    branch = branch,
                    agentOutput = agentOutput,
                    blockedBy = blockedBy,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                    history = listOf(HistoryEntry.StageChange(Stage.Prioritized, stage, createdAt)),
                )
            }

            /** Mirrors `HumanReviewQueue`'s sort key: questions first (FIFO), then approvals by rightmost column, then priority. */
            private fun sortQueue(items: List<HumanReviewItem>): List<HumanReviewItem> {
                val columnOrder = Column.all()
                fun sortKey(item: HumanReviewItem): List<Comparable<*>> =
                    when (val kind = item.kind) {
                        is ReviewItemKind.AgentQuestion -> listOf(0, 0, 0, item.createdAt)
                        is ReviewItemKind.BufferApproval -> {
                            val inverted = columnOrder.size - columnOrder.indexOf(kind.bufferColumn)
                            listOf(1, inverted, kind.taskPriority, item.createdAt)
                        }
                    }
                return items.sortedWith(compareBy({ sortKey(it)[0] as Int }, { sortKey(it)[1] as Int }, { sortKey(it)[2] as Int }, { sortKey(it)[3] as Instant }))
            }
        }
    }
