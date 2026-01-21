package ru.sbrf.dab2c.executor.clients.efs.adapter.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.AuditClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.AuditEventServiceEvent
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseVoid
import ru.sbrf.dab2c.executor.domain.audit.AuditEvent

private val logger = KotlinLogging.logger {}

/**
 * Implementation of AuditClient using Ktor HTTP client.
 */
class AuditClientImpl(
    private val httpClient: HttpClient
) : AuditClient {

    override suspend fun sendEvent(event: AuditEvent, cookie: String) {
        logger.debug { "Sending audit event: ${event.event}" }

        try {
            val request = AuditEventServiceEvent(
                event = event.event,
                success = event.success,
                params = event.params.ifEmpty { null }
            )

            val response: BaseResponseVoid = httpClient.post("/audit/event") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Cookie, cookie)
                setBody(request)
            }.body()

            val errors = response.errors
            if (!errors.isNullOrEmpty()) {
                val errorMessages = errors.joinToString { it.message ?: it.code ?: "Unknown error" }
                logger.warn { "Audit event '${event.event}' completed with errors: $errorMessages" }
            } else {
                logger.debug { "Audit event '${event.event}' sent successfully" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send audit event: ${event.event}" }
            throw e
        }
    }
}
