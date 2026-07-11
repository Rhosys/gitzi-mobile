package ch.rhosys.gitzi.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors `model::task::Stage` in the gitzi daemon. The legacy stages
 * (Backlog/InProgress/WaitingForReview) exist only for tasks created by an
 * older daemon version and always resolve to a column via [toColumn].
 */
@Serializable
enum class Stage {
    @SerialName("backlog") Backlog,
    @SerialName("in-progress") InProgress,
    @SerialName("waiting-for-review") WaitingForReview,
    @SerialName("prioritized") Prioritized,
    @SerialName("designing") Designing,
    @SerialName("coding-buffer") CodingBuffer,
    @SerialName("coding") Coding,
    @SerialName("review-buffer") ReviewBuffer,
    @SerialName("reviewing") Reviewing,
    @SerialName("security-audit-buffer") SecurityAuditBuffer,
    @SerialName("auditing") Auditing,
    @SerialName("deployment-buffer") DeploymentBuffer,
    @SerialName("deploying") Deploying,
    @SerialName("done") Done,
    ;

    fun toColumn(): Column = when (this) {
        Backlog, Prioritized -> Column.Prioritized
        InProgress, Coding -> Column.Coding
        WaitingForReview, ReviewBuffer -> Column.ReviewBuffer
        Designing -> Column.Designing
        CodingBuffer -> Column.CodingBuffer
        Reviewing -> Column.Reviewing
        SecurityAuditBuffer -> Column.SecurityAuditBuffer
        Auditing -> Column.Auditing
        DeploymentBuffer -> Column.DeploymentBuffer
        Deploying -> Column.Deploying
        Done -> Column.Done
    }
}

@Serializable
sealed interface HistoryEntry {
    val at: Instant

    @Serializable
    @SerialName("stage_change")
    data class StageChange(
        val from: Stage,
        val to: Stage,
        override val at: Instant,
        val note: String? = null,
    ) : HistoryEntry

    @Serializable
    @SerialName("approval")
    data class Approval(
        override val at: Instant,
        val targetStage: Stage,
    ) : HistoryEntry

    @Serializable
    @SerialName("rejection")
    data class Rejection(
        override val at: Instant,
        val feedback: String,
        val returnedTo: Stage,
    ) : HistoryEntry
}

@Serializable
data class Task(
    val id: String,
    val epicId: String,
    val title: String,
    val description: String? = null,
    val stage: Stage,
    val priority: Int = 100,
    val agent: String? = null,
    val branch: String? = null,
    val repo: String? = null,
    val agentFeedback: String? = null,
    /** The last agent's raw output text — design doc, review findings, audit report, etc. */
    val agentOutput: String? = null,
    val resumeSummary: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val history: List<HistoryEntry> = emptyList(),
    /** Task IDs that must reach [Stage.Done] before this task is eligible to be picked up. */
    val blockedBy: List<String> = emptyList(),
)
