package ru.sbrf.dab2c.executor.clients.efs.adapter.configuration.audit

import ru.sbrf.dab2c.executor.clients.efs.adapter.api.AuditClient
import ru.sbrf.dab2c.executor.domain.audit.AuditEvent
import ru.sbrf.dab2c.executor.library.audit.port.AuditEventSender

/**
 * EFS-based implementation of [AuditEventSender].
 */
class EfsAuditEventSender(
    private val auditClient: AuditClient
) : AuditEventSender {

    /**
     * Sends the given [AuditEvent] to EFS using [AuditClient].
     */
    override suspend fun sendEvent(event: AuditEvent, cookie: String) {
        auditClient.sendEvent(event, cookie)
    }
}
