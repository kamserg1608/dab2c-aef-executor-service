package ru.sbrf.dab2c.executor.library.audit.model

/** Pairs a success and failed audit event name. */
data class AuditEventNamePair(
    val success: String,
    val failed: String
)
