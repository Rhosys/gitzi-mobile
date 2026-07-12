package ch.rhosys.gitzi.data.mock

import ch.rhosys.gitzi.domain.model.ReviewItemKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mock backend replicates `HumanReviewQueue`'s ordering invariant so the
 * Review screen behaves identically whether it's pointed at demo data or a
 * real deployment: agent questions always outrank buffer approvals.
 */
class MockGitziRepositoryTest {
    @Test
    fun `seeded queue surfaces the agent question before any buffer approval`() =
        runTest {
            val repository = MockGitziRepository()
            val queue = repository.observeReviewQueue().first()

            assertTrue(queue.isNotEmpty())
            assertTrue(queue.first().kind is ReviewItemKind.AgentQuestion)
        }

    @Test
    fun `answering the question resolves it and reveals the next item`() =
        runTest {
            val repository = MockGitziRepository()
            val question = repository.observeReviewQueue().first().first()

            repository.answerReviewItem(question.id, "Per-device — matches both providers' reinstall semantics.")

            val remaining = repository.observeReviewQueue().first()
            assertTrue(remaining.none { it.id == question.id })
            assertTrue(remaining.all { it.kind is ReviewItemKind.BufferApproval })
        }

    @Test
    fun `approving a buffer item removes it from the queue`() =
        runTest {
            val repository = MockGitziRepository()
            val question = repository.observeReviewQueue().first().first()
            repository.answerReviewItem(question.id, "answered")

            val approval = repository.observeReviewQueue().first().first()
            repository.approveReviewItem(approval.id)

            val remaining = repository.observeReviewQueue().first()
            assertTrue(remaining.none { it.id == approval.id })
        }
}
