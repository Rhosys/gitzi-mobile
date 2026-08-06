package ch.rhosys.gitzi.data.remote.dto

import ch.rhosys.gitzi.domain.model.Column
import ch.rhosys.gitzi.domain.model.Stage
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTOs for the assumed Gitzi HTTP + WebSocket API — see docs/api-contract.md.
 * No production Gitzi backend exists yet; this is the contract the mobile app
 * was built against so a future server has a concrete target to implement.
 */
@Serializable
data class EpicDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("task_ids") val taskIds: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
)

@Serializable
sealed interface HistoryEntryDto {
    @Serializable
    @SerialName("stage_change")
    data class StageChange(
        val from: Stage,
        val to: Stage,
        val at: Instant,
        val note: String? = null,
    ) : HistoryEntryDto

    @Serializable
    @SerialName("approval")
    data class Approval(val at: Instant, @SerialName("target_stage") val targetStage: Stage) : HistoryEntryDto

    @Serializable
    @SerialName("rejection")
    data class Rejection(
        val at: Instant,
        val feedback: String,
        @SerialName("returned_to") val returnedTo: Stage,
    ) : HistoryEntryDto
}

@Serializable
data class TaskDto(
    val id: String,
    @SerialName("epic_id") val epicId: String,
    val title: String,
    val description: String? = null,
    val stage: Stage,
    val priority: Int = 100,
    val agent: String? = null,
    val branch: String? = null,
    val repo: String? = null,
    @SerialName("agent_feedback") val agentFeedback: String? = null,
    @SerialName("agent_output") val agentOutput: String? = null,
    @SerialName("resume_summary") val resumeSummary: String? = null,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
    val history: List<HistoryEntryDto> = emptyList(),
    @SerialName("blocked_by") val blockedBy: List<String> = emptyList(),
)

@Serializable
sealed interface ReviewItemKindDto {
    @Serializable
    @SerialName("agent_question")
    data class AgentQuestion(val question: String) : ReviewItemKindDto

    @Serializable
    @SerialName("buffer_approval")
    data class BufferApproval(
        @SerialName("buffer_column") val bufferColumn: Column,
        @SerialName("task_priority") val taskPriority: Int,
    ) : ReviewItemKindDto
}

@Serializable
data class ReviewItemDto(
    val id: String,
    @SerialName("task_id") val taskId: String,
    val kind: ReviewItemKindDto,
    @SerialName("created_at") val createdAt: Instant,
)

@Serializable
enum class ChatRoleDto {
    @SerialName("user") User,
    @SerialName("system") System,
    @SerialName("agent") Agent,
}

@Serializable
data class ChatMessageDto(
    val id: String,
    @SerialName("session_id") val sessionId: String,
    val role: ChatRoleDto,
    val content: String,
    val ts: Instant,
)

@Serializable
enum class ProviderKindDto {
    @SerialName("openai-compatible") OpenAiCompatible,
    @SerialName("bedrock") Bedrock,
}

@Serializable
data class ProviderDto(
    val name: String,
    val kind: ProviderKindDto = ProviderKindDto.OpenAiCompatible,
    @SerialName("api_url") val apiUrl: String = "",
    @SerialName("api_key_set") val apiKeySet: Boolean = false,
    val region: String? = null,
    @SerialName("sso_start_url") val ssoStartUrl: String? = null,
    @SerialName("sso_account_id") val ssoAccountId: String? = null,
    @SerialName("sso_role_name") val ssoRoleName: String? = null,
    @SerialName("model_id") val modelId: String? = null,
    val enabled: Boolean = true,
)

@Serializable
data class AgentDefDto(
    val role: String,
    val model: String,
    @SerialName("api_url") val apiUrl: String? = null,
    val provider: String? = null,
)

@Serializable
data class RepoConfigDto(
    val path: String,
    @SerialName("merge_strategy") val mergeStrategy: String = "ff-only",
    @SerialName("main_branch") val mainBranch: String? = null,
)

@Serializable
data class ConfigDto(
    @SerialName("wip_limits") val wipLimits: Map<String, Int> = emptyMap(),
    val providers: List<ProviderDto> = emptyList(),
    @SerialName("fallback_provider") val fallbackProvider: String? = null,
    val agents: List<AgentDefDto> = emptyList(),
    val repos: List<RepoConfigDto> = emptyList(),
)

// ── Request bodies ─────────────────────────────────────────────────────────

@Serializable
data class CreateEpicRequest(val title: String, val description: String? = null)

@Serializable
data class CreateTaskRequest(
    @SerialName("epic_id") val epicId: String,
    val title: String,
    val description: String? = null,
    val priority: Int? = null,
    val repo: String? = null,
)

@Serializable
data class UpdateTaskRequest(val title: String? = null, val description: String? = null)

@Serializable
data class ParkTaskRequest(val reason: String)

@Serializable
data class BlockTaskRequest(@SerialName("blocked_by") val blockedBy: List<String>)

@Serializable
data class AnswerReviewRequest(val content: String)

@Serializable
data class RejectReviewRequest(val feedback: String)

@Serializable
data class SendChatRequest(val content: String)

@Serializable
data class EditChatMessageRequest(val content: String)

@Serializable
data class UpdateConfigRequest(
    @SerialName("wip_limits") val wipLimits: Map<String, Int>,
    val agents: List<AgentDefDto>,
    val repos: List<RepoConfigDto>,
    @SerialName("fallback_provider") val fallbackProvider: String? = null,
)
