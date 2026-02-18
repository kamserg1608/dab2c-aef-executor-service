package ru.sbrf.dab2c.executor.clients.efs.adapter.configuration.audit

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.AuditClient
import ru.sbrf.dab2c.executor.library.audit.model.AuditEvent
import ru.sbrf.dab2c.executor.library.audit.port.AuditEventSender

private val logger = KotlinLogging.logger {}

/**
 * EFS-based implementation of [AuditEventSender].
 */
@Service
class EfsAuditEventSender(
    private val auditClient: AuditClient
) : AuditEventSender {

    /**
     * Sends the given [AuditEvent] to EFS using [AuditClient].
     */
    @Suppress("SwallowedException")
    override suspend fun sendEvent(event: AuditEvent, cookie: String) {
        logger.trace { "Sending audit event=${event.event}, success=${event.success}" }
        try {
            auditClient.sendEvent(event, cookie)
            logger.trace { "Audit event sent successfully: ${event.event}" }
        } catch (t: Throwable) {
            logger.error(t) { "Failed to send audit event=${event.event}" }
        }
    }
}
