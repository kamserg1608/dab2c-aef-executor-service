package ru.sbrf.dab2c.executor.library.audit.port

import ru.sbrf.dab2c.executor.domain.audit.AuditEvent

/**
 * Port for sending audit events to external system.
 * Implementation should live in client/adapters modules (e.g. efs-adapter).
 */
fun interface AuditEventSender {
    suspend fun sendEvent(event: AuditEvent, cookie: String)
}
