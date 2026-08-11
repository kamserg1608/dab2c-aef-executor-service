package ru.sbrf.dab2c.executor.library.audit

import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.library.audit.model.AuditEvent
import ru.sbrf.dab2c.executor.library.audit.model.AuditEventNamePair
import ru.sbrf.dab2c.executor.library.audit.model.AuditParams
import ru.sbrf.dab2c.executor.library.audit.model.InteractionAuditRequest
import ru.sbrf.dab2c.executor.library.audit.port.AuditEventSender

class InteractionAuditorImplTest {

    private val eventNames = AuditEventNamePair(
        success = "TEST_SUCCESS",
        failed = "TEST_FAILED"
    )
    private val senderValue = "test-sender"
    private val receiver = "test-receiver"
    private val auditEventSender = mockk<AuditEventSender>(relaxed = true)

    private val auditor = InteractionAuditorImpl(
        eventNames = eventNames,
        senderValue = senderValue,
        receiver = receiver,
        auditEventSender = auditEventSender
    )

    @Test
    fun `should send success event with correct name and params`() = runTest {
        val request = InteractionAuditRequest(
            answerCode = "200",
            rqMessage = "request body",
            rsMessage = "response body",
            errorCode = "ERR_01",
            errorTitle = "Some error"
        )

        auditor.success(request)

        val eventSlot = slot<AuditEvent>()
        coVerify { auditEventSender.sendEvent(capture(eventSlot)) }

        val captured = eventSlot.captured
        assertThat(captured.event).isEqualTo("TEST_SUCCESS")
        assertThat(captured.success).isTrue()
        assertThat(captured.params).containsEntry(AuditParams.SENDER, senderValue)
        assertThat(captured.params).containsEntry(AuditParams.RECEIVER, receiver)
        assertThat(captured.params).containsEntry(AuditParams.ANSWER_CODE, "200")
        assertThat(captured.params).containsEntry(AuditParams.RQ_MESSAGE, "request body")
        assertThat(captured.params).containsEntry(AuditParams.RS_MESSAGE, "response body")
        assertThat(captured.params).containsEntry(AuditParams.ERROR_CODE, "ERR_01")
        assertThat(captured.params).containsEntry(AuditParams.ERROR_TITLE, "Some error")
    }

    @Test
    fun `should send failed event with correct name and params`() = runTest {
        val request = InteractionAuditRequest(
            answerCode = "500",
            rqMessage = "request body",
            rsMessage = "response body",
            errorCode = "ERR_02",
            errorTitle = "Server error"
        )

        auditor.failed(request)

        val eventSlot = slot<AuditEvent>()
        coVerify { auditEventSender.sendEvent(capture(eventSlot)) }

        val captured = eventSlot.captured
        assertThat(captured.event).isEqualTo("TEST_FAILED")
        assertThat(captured.success).isFalse()
        assertThat(captured.params).containsEntry(AuditParams.SENDER, senderValue)
        assertThat(captured.params).containsEntry(AuditParams.RECEIVER, receiver)
        assertThat(captured.params).containsEntry(AuditParams.ANSWER_CODE, "500")
        assertThat(captured.params).containsEntry(AuditParams.RQ_MESSAGE, "request body")
        assertThat(captured.params).containsEntry(AuditParams.RS_MESSAGE, "response body")
        assertThat(captured.params).containsEntry(AuditParams.ERROR_CODE, "ERR_02")
        assertThat(captured.params).containsEntry(AuditParams.ERROR_TITLE, "Server error")
    }

    @Test
    fun `should omit blank and null fields from params`() = runTest {
        val request = InteractionAuditRequest(
            answerCode = "200",
            rqMessage = null,
            errorCode = "   ",
            errorTitle = ""
        )

        auditor.success(request)

        val eventSlot = slot<AuditEvent>()
        coVerify { auditEventSender.sendEvent(capture(eventSlot)) }

        val params = eventSlot.captured.params
        assertThat(params).containsOnlyKeys(
            AuditParams.SENDER,
            AuditParams.RECEIVER,
            AuditParams.ANSWER_CODE
        )
    }

    @Test
    fun `should include sender and receiver in params`() = runTest {
        val request = InteractionAuditRequest(answerCode = "200")

        auditor.success(request)

        val eventSlot = slot<AuditEvent>()
        coVerify { auditEventSender.sendEvent(capture(eventSlot)) }

        val params = eventSlot.captured.params
        assertThat(params).containsEntry(AuditParams.SENDER, senderValue)
        assertThat(params).containsEntry(AuditParams.RECEIVER, receiver)
    }
}
