package ch.rhosys.gitzi.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MergeStrategy {
    @SerialName("ff-only") FfOnly,
    @SerialName("gitzi-branch") GitziBranch,
    @SerialName("merge-commit") MergeCommit,
    @SerialName("pull-request") PullRequest,
    @SerialName("push-to-remote") PushToRemote,
}

@Serializable
data class RepoConfig(
    val path: String,
    val mergeStrategy: MergeStrategy = MergeStrategy.FfOnly,
    val mainBranch: String? = null,
)

@Serializable
data class WipLimits(val overrides: Map<String, Int> = emptyMap())

@Serializable
data class GitziConfig(
    val wipLimits: WipLimits = WipLimits(),
    val providers: List<ProviderDef> = emptyList(),
    val fallbackProvider: String? = null,
    val agents: List<AgentDef> = emptyList(),
    val repos: List<RepoConfig> = emptyList(),
)
