package ru.sbrf.dab2c.executor.it.support.wiremock

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Waits on audit events posted to the EFS adapter mock. */
object AuditEventAwaiter {

    private const val AUDIT_EVENT_URL = "/audit/event"

    /** Waits for an audit event named [event] whose params satisfy [matches], returning those params. */
    suspend fun WireMockServer.awaitAuditEvent(
        event: String,
        timeout: Duration = 10.seconds,
        pollInterval: Duration = 50.milliseconds,
        matches: (Map<String, String>) -> Boolean = { true }
    ): Map<String, String> = withTimeout(timeout) {
        var found = auditParams(event).firstOrNull(matches)
        while (found == null) {
            delay(pollInterval)
            found = auditParams(event).firstOrNull(matches)
        }
        found
    }

    @Suppress("UNCHECKED_CAST")
    private fun WireMockServer.auditParams(event: String): List<Map<String, String>> =
        findAll(postRequestedFor(urlEqualTo(AUDIT_EVENT_URL)))
            .map { ObjectMappers.MAPPER.readValue<Map<String, Any?>>(it.bodyAsString) }
            .filter { it["event"] == event }
            .map { it["params"] as? Map<String, String> ?: emptyMap() }
}
