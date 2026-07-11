package ch.rhosys.gitzi.data.remote.dto

import ch.rhosys.gitzi.domain.model.AgentDef
import ch.rhosys.gitzi.domain.model.AgentRole
import ch.rhosys.gitzi.domain.model.ChatMessage
import ch.rhosys.gitzi.domain.model.ChatRole
import ch.rhosys.gitzi.domain.model.Epic
import ch.rhosys.gitzi.domain.model.GitziConfig
import ch.rhosys.gitzi.domain.model.HistoryEntry
import ch.rhosys.gitzi.domain.model.HumanReviewItem
import ch.rhosys.gitzi.domain.model.MergeStrategy
import ch.rhosys.gitzi.domain.model.ProviderDef
import ch.rhosys.gitzi.domain.model.ProviderKind
import ch.rhosys.gitzi.domain.model.RepoConfig
import ch.rhosys.gitzi.domain.model.ReviewItemKind
import ch.rhosys.gitzi.domain.model.Task
import ch.rhosys.gitzi.domain.model.WipLimits

fun EpicDto.toDomain() = Epic(id, title, description, taskIds, createdAt, updatedAt)

fun HistoryEntryDto.toDomain(): HistoryEntry =
    when (this) {
        is HistoryEntryDto.StageChange -> HistoryEntry.StageChange(from, to, at, note)
        is HistoryEntryDto.Approval -> HistoryEntry.Approval(at, targetStage)
        is HistoryEntryDto.Rejection -> HistoryEntry.Rejection(at, feedback, returnedTo)
    }

fun TaskDto.toDomain() =
    Task(
        id = id,
        epicId = epicId,
        title = title,
        description = description,
        stage = stage,
        priority = priority,
        agent = agent,
        branch = branch,
        repo = repo,
        agentFeedback = agentFeedback,
        agentOutput = agentOutput,
        resumeSummary = resumeSummary,
        createdAt = createdAt,
        updatedAt = updatedAt,
        history = history.map { it.toDomain() },
        blockedBy = blockedBy,
    )

fun ReviewItemKindDto.toDomain(): ReviewItemKind =
    when (this) {
        is ReviewItemKindDto.AgentQuestion -> ReviewItemKind.AgentQuestion(question)
        is ReviewItemKindDto.BufferApproval -> ReviewItemKind.BufferApproval(bufferColumn, taskPriority)
    }

fun ReviewItemDto.toDomain() = HumanReviewItem(id, taskId, kind.toDomain(), createdAt)

fun ChatRoleDto.toDomain() =
    when (this) {
        ChatRoleDto.User -> ChatRole.User
        ChatRoleDto.System -> ChatRole.System
        ChatRoleDto.Agent -> ChatRole.Agent
    }

fun ChatMessageDto.toDomain() = ChatMessage(role.toDomain(), content, ts)

fun ProviderKindDto.toDomain() =
    when (this) {
        ProviderKindDto.OpenAiCompatible -> ProviderKind.OpenAiCompatible
        ProviderKindDto.Bedrock -> ProviderKind.Bedrock
    }

fun ProviderDef.toDto() =
    ProviderDto(
        name = name,
        kind = when (kind) {
            ProviderKind.OpenAiCompatible -> ProviderKindDto.OpenAiCompatible
            ProviderKind.Bedrock -> ProviderKindDto.Bedrock
        },
        apiUrl = apiUrl,
        apiKeySet = apiKeySet,
        region = region,
        ssoStartUrl = ssoStartUrl,
        ssoAccountId = ssoAccountId,
        ssoRoleName = ssoRoleName,
        modelId = modelId,
        enabled = enabled,
    )

fun ProviderDto.toDomain() =
    ProviderDef(
        name = name,
        kind = kind.toDomain(),
        apiUrl = apiUrl,
        apiKeySet = apiKeySet,
        region = region,
        ssoStartUrl = ssoStartUrl,
        ssoAccountId = ssoAccountId,
        ssoRoleName = ssoRoleName,
        modelId = modelId,
        enabled = enabled,
    )

private fun agentRoleFromWire(role: String): AgentRole =
    AgentRole.entries.firstOrNull { it.name.equals(role, ignoreCase = true) } ?: AgentRole.Main

fun AgentDefDto.toDomain() = AgentDef(agentRoleFromWire(role), model, apiUrl, provider)

fun AgentDef.toDto() = AgentDefDto(role.name.lowercase(), model, apiUrl, provider)

private fun mergeStrategyFromWire(value: String): MergeStrategy =
    when (value) {
        "gitzi-branch" -> MergeStrategy.GitziBranch
        "merge-commit" -> MergeStrategy.MergeCommit
        "pull-request" -> MergeStrategy.PullRequest
        "push-to-remote" -> MergeStrategy.PushToRemote
        else -> MergeStrategy.FfOnly
    }

private fun MergeStrategy.toWire(): String =
    when (this) {
        MergeStrategy.FfOnly -> "ff-only"
        MergeStrategy.GitziBranch -> "gitzi-branch"
        MergeStrategy.MergeCommit -> "merge-commit"
        MergeStrategy.PullRequest -> "pull-request"
        MergeStrategy.PushToRemote -> "push-to-remote"
    }

fun RepoConfigDto.toDomain() = RepoConfig(path, mergeStrategyFromWire(mergeStrategy), mainBranch)

fun RepoConfig.toDto() = RepoConfigDto(path, mergeStrategy.toWire(), mainBranch)

fun ConfigDto.toDomain() =
    GitziConfig(
        wipLimits = WipLimits(wipLimits),
        providers = providers.map { it.toDomain() },
        fallbackProvider = fallbackProvider,
        agents = agents.map { it.toDomain() },
        repos = repos.map { it.toDomain() },
    )
