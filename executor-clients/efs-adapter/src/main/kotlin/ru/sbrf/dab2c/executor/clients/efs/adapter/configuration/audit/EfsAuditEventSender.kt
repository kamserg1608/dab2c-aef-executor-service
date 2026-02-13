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
    override suspend fun sendEvent(event: AuditEvent, cookie: String) {
        try {
            auditClient.sendEvent(event, cookie)
        } catch (e: CancellationException) {
            if (e.message?.contains("client", ignoreCase = true) != true) {
                logger.warn(e) {
                    "Audit sending cancelled unexpectedly for event=${event.event}"
                }
            }
        } catch (t: Throwable) {
            logger.error(t) {
                "Failed to send audit event=${event.event}"
            }
        }
    }
}
