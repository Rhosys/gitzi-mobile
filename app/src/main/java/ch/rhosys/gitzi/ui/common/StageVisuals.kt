package ch.rhosys.gitzi.ui.common

import androidx.compose.ui.graphics.Color
import ch.rhosys.gitzi.domain.model.Column
import ch.rhosys.gitzi.ui.theme.StageAuditing
import ch.rhosys.gitzi.ui.theme.StageBuffer
import ch.rhosys.gitzi.ui.theme.StageCoding
import ch.rhosys.gitzi.ui.theme.StageDeploying
import ch.rhosys.gitzi.ui.theme.StageDesigning
import ch.rhosys.gitzi.ui.theme.StageDone
import ch.rhosys.gitzi.ui.theme.StagePrioritized
import ch.rhosys.gitzi.ui.theme.StageReviewing

fun Column.color(): Color =
    when (this) {
        Column.Prioritized -> StagePrioritized
        Column.Designing -> StageDesigning
        Column.CodingBuffer -> StageBuffer
        Column.Coding -> StageCoding
        Column.ReviewBuffer -> StageBuffer
        Column.Reviewing -> StageReviewing
        Column.SecurityAuditBuffer -> StageBuffer
        Column.Auditing -> StageAuditing
        Column.DeploymentBuffer -> StageBuffer
        Column.Deploying -> StageDeploying
        Column.Done -> StageDone
    }
