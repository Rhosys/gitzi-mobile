package ch.rhosys.gitzi.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The kind of human attention a [HumanReviewItem] needs. Mirrors
 * `dispatcher::review_queue::ReviewItemKind` in the gitzi daemon.
 */
@Serializable
sealed interface ReviewItemKind {
    @Serializable
    @SerialName("agent_question")
    data class AgentQuestion(val question: String) : ReviewItemKind

    @Serializable
    @SerialName("buffer_approval")
    data class BufferApproval(val bufferColumn: Column, val taskPriority: Int) : ReviewItemKind
}

/**
 * A single item in the human review queue — the one thing the app should ever
 * surface for action at a time. See `HumanReviewQueue` in the daemon: agent
 * questions always outrank buffer approvals, and only the single topmost item
 * is ever "visible".
 */
@Serializable
data class HumanReviewItem(
    val id: String,
    val taskId: String,
    val kind: ReviewItemKind,
    val createdAt: Instant,
)
