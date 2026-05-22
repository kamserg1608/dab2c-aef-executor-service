package ru.sbrf.dab2c.executor.voice.audit

import io.grpc.Status
import io.grpc.StatusException
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.library.audit.model.InteractionAuditRequest
import ru.sbrf.dab2c.executor.library.audit.port.InteractionAuditor
import ru.sbrf.dab2c.executor.voice.session.observer.Replica
import ru.sbrf.dab2c.executor.voice.session.observer.TurnCompleted

class DialogTurnAuditorTest {

    private val interactionAuditor = mockk<InteractionAuditor>(relaxed = true)
    private val auditor = DialogTurnAuditor(interactionAuditor)

    @Test
    fun `onTurnCompleted emits success audit with replica texts`() = runTest {
        auditor.onTurnCompleted(
            TurnCompleted(
                userReplica = Replica("Hello", 1L, 2L),
                assistantReplica = Replica("World", 3L, 4L),
                turnEvents = emptyList(),
                totalTokens = null,
            )
        )

        val slot = slot<InteractionAuditRequest>()
        coVerify(exactly = 1) { interactionAuditor.success(capture(slot)) }
        assertThat(slot.captured.answerCode).isEqualTo("200")
        assertThat(slot.captured.rqMessage).isEqualTo("Hello")
        assertThat(slot.captured.rsMessage).isEqualTo("World")
    }

    @Test
    fun `onTurnCompleted treats null replicas as empty strings`() = runTest {
        auditor.onTurnCompleted(
            TurnCompleted(
                userReplica = null,
                assistantReplica = null,
                turnEvents = emptyList(),
                totalTokens = null,
            )
        )

        val slot = slot<InteractionAuditRequest>()
        coVerify(exactly = 1) { interactionAuditor.success(capture(slot)) }
        assertThat(slot.captured.rqMessage).isEmpty()
        assertThat(slot.captured.rsMessage).isEmpty()
    }

    @Test
    fun `onSessionCompleted with null cause emits nothing`() = runTest {
        auditor.onSessionCompleted(cause = null)

        coVerify(exactly = 0) { interactionAuditor.failed(any()) }
        coVerify(exactly = 0) { interactionAuditor.success(any()) }
    }

    @Test
    fun `onSessionCompleted with tolerant CancellationException emits nothing`() = runTest {
        auditor.onSessionCompleted(cause = CancellationException("client cancelled"))

        coVerify(exactly = 0) { interactionAuditor.failed(any()) }
    }

    @Test
    fun `onSessionCompleted with tolerant StatusException UNAVAILABLE emits nothing`() = runTest {
        auditor.onSessionCompleted(cause = StatusException(Status.UNAVAILABLE.withDescription("rst")))

        coVerify(exactly = 0) { interactionAuditor.failed(any()) }
    }

    @Test
    fun `onSessionCompleted with non-tolerant RuntimeException emits failed audit`() = runTest {
        auditor.onSessionCompleted(cause = RuntimeException("stream failure"))

        val slot = slot<InteractionAuditRequest>()
        coVerify(exactly = 1) { interactionAuditor.failed(capture(slot)) }
        assertThat(slot.captured.answerCode).isEqualTo("500")
        assertThat(slot.captured.errorCode).isEqualTo("RuntimeException")
        assertThat(slot.captured.errorTitle).isEqualTo("stream failure")
    }

    @Test
    fun `onSessionCompleted with StatusException INTERNAL emits failed audit`() = runTest {
        auditor.onSessionCompleted(cause = StatusException(Status.INTERNAL.withDescription("boom")))

        val slot = slot<InteractionAuditRequest>()
        coVerify(exactly = 1) { interactionAuditor.failed(capture(slot)) }
        assertThat(slot.captured.errorCode).isEqualTo("INTERNAL")
    }
}
