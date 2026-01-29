package ru.sbrf.dab2c.executor.clients.efs.adapter.impl

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import ru.sbrf.dab2c.executor.clients.common.util.buildFullUrl
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.AuditClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.AuditEventServiceEvent
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseVoid
import ru.sbrf.dab2c.executor.domain.audit.AuditEvent
import ru.sbrf.dab2c.executor.logging.IntegrationLogger

private val logger = KotlinLogging.logger {}

private const val HTTP_OK = 200
private const val CLASS_NAME = "AuditClient"
private const val AUDIT_EVENT_ENDPOINT = "/audit/event"

/**
 * Implementation of AuditClient using Ktor HTTP client.
 */
class AuditClientImpl(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
    private val baseUrl: String
) : AuditClient {

    override suspend fun sendEvent(event: AuditEvent, cookie: String) {
        logger.debug { "Sending audit event: ${event.event}" }

        val request = AuditEventServiceEvent(
            event = event.event,
            success = event.success,
            params = event.params.ifEmpty { null }
        )
        val requestJson = objectMapper.writeValueAsString(request)

        val response = IntegrationLogger.logHttpCallSuspend(
            destinationSystem = baseUrl,
            destinationService = AUDIT_EVENT_ENDPOINT,
            rqMessage = requestJson,
            className = CLASS_NAME,
            responseExtractor = { resp: BaseResponseVoid ->
                objectMapper.writeValueAsString(resp) to HTTP_OK
            }
        ) {
            httpClient.post(buildFullUrl(baseUrl, AUDIT_EVENT_ENDPOINT)) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Cookie, cookie)
                setBody(request)
            }.body<BaseResponseVoid>()
        }

        val errors = response.errors
        if (!errors.isNullOrEmpty()) {
            val errorMessages = errors.joinToString { it.message ?: it.code ?: "Unknown error" }
            logger.warn { "Audit event '${event.event}' completed with errors: $errorMessages" }
        }
    }
}
