package ch.rhosys.gitzi.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Epic(
    val id: String,
    val title: String,
    val description: String? = null,
    val taskIds: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
)
