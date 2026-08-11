package ru.sbrf.dab2c.executor.clients.efs.adapter.configuration.audit

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.AuditClient
import ru.sbrf.dab2c.executor.library.audit.model.AuditEvent

/**
 * Verifies that [EfsAuditEventSender] correctly delegates to [AuditClient]
 * and swallows exceptions without propagating them to callers.
 */
class EfsAuditEventSenderTest {

    private val auditClient: AuditClient = mockk()
    private val sender = EfsAuditEventSender(auditClient)

    @Test
    fun `should delegate audit event to AuditClient`() = runTest {
        val event = AuditEvent(event = "test-event", success = true, params = mapOf("key" to "value"))
        coEvery { auditClient.sendEvent(event) } returns Unit

        sender.sendEvent(event)

        coVerify(exactly = 1) { auditClient.sendEvent(event) }
    }

    @Test
    fun `should not propagate exception when AuditClient throws`() = runTest {
        val event = AuditEvent(event = "failing-event", success = false)
        coEvery { auditClient.sendEvent(event) } throws RuntimeException("send failed")

        sender.sendEvent(event)

        coVerify(exactly = 1) { auditClient.sendEvent(event) }
    }
}
