package ru.sbrf.dab2c.executor.library.audit.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.AuditClient
import ru.sbrf.dab2c.executor.domain.audit.AuditEvent
import ru.sbrf.dab2c.executor.library.audit.context.AuditContextProvider

private val logger = KotlinLogging.logger {}

/**
 * Low-level component responsible for sending
 * audit events to external audit system.
 */
class AuditSender(
    private val auditClient: AuditClient,
    private val contextProvider: AuditContextProvider,
    private val enabled: Boolean
) {
    /**
     * Sends audit event to external system.
     *
     * If audit is disabled or cookie is not available, the event is skipped.
     */
    suspend fun send(event: AuditEvent) {
        if (!enabled) return

        val cookie = contextProvider.cookieOrNull()
        if (cookie.isNullOrBlank()) {
            logger.debug { "Audit cookie is empty -> skip event=${event.event}" }
            return
        }

        runCatching { auditClient.sendEvent(event, cookie) }
            .onFailure { e -> logger.warn(e) { "Failed to send audit event=${event.event}" } }
    }
}
