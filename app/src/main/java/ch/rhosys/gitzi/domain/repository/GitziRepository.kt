package ch.rhosys.gitzi.domain.repository

import ch.rhosys.gitzi.domain.model.ChatMessage
import ch.rhosys.gitzi.domain.model.Epic
import ch.rhosys.gitzi.domain.model.GitziConfig
import ch.rhosys.gitzi.domain.model.HumanReviewItem
import ch.rhosys.gitzi.domain.model.ProviderDef
import ch.rhosys.gitzi.domain.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * The single gateway between the UI and a Gitzi backend. Two implementations
 * exist: [ch.rhosys.gitzi.data.remote.RemoteGitziRepository] talks to the real
 * (assumed) HTTP + WebSocket API, and the debug-only in-memory
 * `MockGitziRepository` lets the whole app be explored with no backend
 * deployed. See docs/api-contract.md for the wire contract this assumes.
 *
 * The `observe*` flows are the live board/queue/chat projections — they
 * update from WebSocket events in the remote implementation and from local
 * mutation in the mock one. Everything else is a one-shot command.
 */
interface GitziRepository {
    fun observeEpics(): Flow<List<Epic>>

    fun observeTasks(): Flow<List<Task>>

    /**
     * The full review queue, already ordered exactly like the daemon's
     * `HumanReviewQueue`: agent questions before buffer approvals, oldest
     * first. UI must only ever act on `first()` — see the "one thing at a
     * time" principle in CLAUDE.md. The rest is just a "N more waiting" count.
     */
    fun observeReviewQueue(): Flow<List<HumanReviewItem>>

    fun observeChat(): Flow<List<ChatMessage>>

    fun observeConfig(): Flow<GitziConfig>

    suspend fun refreshAll(): Result<Unit>

    suspend fun testConnection(): Result<Unit>

    suspend fun createEpic(title: String, description: String?): Result<Epic>

    suspend fun createTask(
        epicId: String,
        title: String,
        description: String?,
        priority: Int?,
        repo: String?,
    ): Result<Task>

    suspend fun updateTask(taskId: String, title: String?, description: String?): Result<Task>

    suspend fun parkTask(taskId: String, reason: String): Result<Unit>

    suspend fun blockTask(taskId: String, blockedBy: List<String>): Result<Unit>

    suspend fun answerReviewItem(itemId: String, content: String): Result<Unit>

    suspend fun approveReviewItem(itemId: String): Result<Unit>

    suspend fun rejectReviewItem(itemId: String, feedback: String): Result<Unit>

    suspend fun sendChatMessage(content: String): Result<Unit>

    suspend fun updateConfig(config: GitziConfig): Result<Unit>

    suspend fun discoverProviders(): Result<List<ProviderDef>>

    suspend fun activateProvider(name: String): Result<Unit>
}
