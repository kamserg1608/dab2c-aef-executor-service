package ru.sbrf.dab2c.executor.clients.efs.adapter.configuration.audit

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
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
    override suspend fun sendEvent(event: AuditEvent) {
        logger.trace { "Sending audit event=${event.event}, success=${event.success}" }
        try {
            auditClient.sendEvent(event)
            logger.trace { "Audit event sent successfully: ${event.event}" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Failed to send audit event=${event.event}" }
        }
    }
}
