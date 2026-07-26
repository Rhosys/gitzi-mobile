package ch.rhosys.gitzi.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ChatRole {
    @SerialName("user") User,
    @SerialName("system") System,
    @SerialName("agent") Agent,
}

@Serializable
data class ChatMessage(
    val id: String,
    val sessionId: String,
    val role: ChatRole,
    val content: String,
    val ts: Instant,
)
