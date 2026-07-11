package ch.rhosys.gitzi.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A Kanban board column. Mirrors `dispatcher::Column` in the gitzi daemon. */
@Serializable
enum class Column(val label: String) {
    @SerialName("prioritized") Prioritized("Prioritized"),
    @SerialName("designing") Designing("Designing"),
    @SerialName("coding-buffer") CodingBuffer("Coding buffer"),
    @SerialName("coding") Coding("Coding"),
    @SerialName("review-buffer") ReviewBuffer("Review buffer"),
    @SerialName("reviewing") Reviewing("Reviewing"),
    @SerialName("security-audit-buffer") SecurityAuditBuffer("Security audit buffer"),
    @SerialName("auditing") Auditing("Auditing"),
    @SerialName("deployment-buffer") DeploymentBuffer("Deployment buffer"),
    @SerialName("deploying") Deploying("Deploying"),
    @SerialName("done") Done("Done"),
    ;

    val isBuffer: Boolean
        get() = this in BUFFERS

    companion object {
        private val BUFFERS = setOf(CodingBuffer, ReviewBuffer, SecurityAuditBuffer, DeploymentBuffer)

        /** Board display order, left to right — matches `Column::all()`. */
        fun all(): List<Column> = entries.toList()

        /** Non-buffer columns, the ones rendered as full board lanes on a phone. */
        fun laneColumns(): List<Column> = entries.filterNot { it.isBuffer }
    }
}
