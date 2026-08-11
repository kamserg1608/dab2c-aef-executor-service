package ru.sbrf.dab2c.executor.library.audit.model

/**
 * Audit event to be sent to EFS Adapter for tracking and logging.
 */
data class AuditEvent(
    val event: String,
    val success: Boolean,
    val params: Map<String, String> = emptyMap()
)
