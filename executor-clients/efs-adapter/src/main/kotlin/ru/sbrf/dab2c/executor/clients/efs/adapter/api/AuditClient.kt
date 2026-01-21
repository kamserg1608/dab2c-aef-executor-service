package ru.sbrf.dab2c.executor.clients.efs.adapter.api

import ru.sbrf.dab2c.executor.domain.audit.AuditEvent

/**
 * Client interface for sending audit events to EFS Adapter.
 */
interface AuditClient {

    /**
     * Send an audit event.
     */
    suspend fun sendEvent(event: AuditEvent, cookie: String)
}
