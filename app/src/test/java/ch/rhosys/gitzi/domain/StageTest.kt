package ch.rhosys.gitzi.domain

import ch.rhosys.gitzi.domain.model.Column
import ch.rhosys.gitzi.domain.model.Stage
import org.junit.Assert.assertEquals
import org.junit.Test

class StageTest {
    @Test
    fun `column-aligned stages map to their own column`() {
        assertEquals(Column.Designing, Stage.Designing.toColumn())
        assertEquals(Column.Coding, Stage.Coding.toColumn())
        assertEquals(Column.ReviewBuffer, Stage.ReviewBuffer.toColumn())
        assertEquals(Column.Reviewing, Stage.Reviewing.toColumn())
        assertEquals(Column.Done, Stage.Done.toColumn())
    }

    @Test
    fun `legacy stages map to their closest column, same as the daemon`() {
        assertEquals(Column.Prioritized, Stage.Backlog.toColumn())
        assertEquals(Column.Coding, Stage.InProgress.toColumn())
        assertEquals(Column.ReviewBuffer, Stage.WaitingForReview.toColumn())
    }
}
