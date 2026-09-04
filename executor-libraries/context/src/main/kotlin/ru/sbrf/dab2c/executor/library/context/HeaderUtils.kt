package ru.sbrf.dab2c.executor.library.context

import java.util.UUID

/** Utilities for validating and normalizing request headers. */
internal object HeaderUtils {

    private val uuidPattern = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    )
    private val telemetryTraceIdPattern = Regex(
        "^([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})$"
    )

    /** Returns trace ID in UUID format, generating a new UUID for missing or invalid values. */
    fun normalizeTraceId(traceId: String?): String {
        val value = traceId?.trim()

        return when {
            value.isNullOrEmpty() -> UUID.randomUUID().toString()
            uuidPattern.matches(value) -> UUID.fromString(value).toString()
            else -> telemetryTraceIdPattern.matchEntire(value)
                ?.let { match ->
                    UUID.fromString(match.groupValues.drop(1).joinToString("-")).toString()
                }
                ?: UUID.randomUUID().toString()
        }
    }
}
