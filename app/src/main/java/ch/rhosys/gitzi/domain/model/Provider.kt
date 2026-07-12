package ch.rhosys.gitzi.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Which kind of model server a provider talks to. Mirrors `config::ProviderKind`.
 *
 * Covers the deployment shapes gitzi is expected to support once the backend
 * exists: the operator's own OpenAI-compatible endpoint (bring-your-own-model,
 * a self-hosted Ollama box on EC2, LM Studio, on-prem gitzi, etc.) or AWS Bedrock.
 */
@Serializable
enum class ProviderKind {
    @SerialName("openai-compatible") OpenAiCompatible,
    @SerialName("bedrock") Bedrock,
}

@Serializable
data class ProviderDef(
    val name: String,
    val kind: ProviderKind = ProviderKind.OpenAiCompatible,
    val apiUrl: String = "",
    /** Never the raw secret over the wire in a real deployment — a masked placeholder once set. */
    val apiKeySet: Boolean = false,
    val region: String? = null,
    val ssoStartUrl: String? = null,
    val ssoAccountId: String? = null,
    val ssoRoleName: String? = null,
    val modelId: String? = null,
    val enabled: Boolean = true,
)

/** Mirrors `dispatcher::AgentRole` plus the always-present `main` role. */
@Serializable
enum class AgentRole {
    @SerialName("main") Main,
    @SerialName("prioritizer") Prioritizer,
    @SerialName("designer") Designer,
    @SerialName("coder") Coder,
    @SerialName("reviewer") Reviewer,
    @SerialName("auditor") Auditor,
    @SerialName("infrarian") Infrarian,
    ;

    companion object {
        fun all(): List<AgentRole> = entries.toList()
    }
}

@Serializable
data class AgentDef(
    val role: AgentRole,
    val model: String,
    val apiUrl: String? = null,
    val provider: String? = null,
)
