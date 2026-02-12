package ru.sbrf.dab2c.executor.library.audit.port

import ru.sbrf.dab2c.executor.library.audit.model.AuditEvent

/**
 * Port for sending audit events to an external audit system.
 */
fun interface AuditEventSender {

    /**
     * Sends the given [AuditEvent] to the external audit system.
     */
    suspend fun sendEvent(event: AuditEvent, cookie: String)
}
