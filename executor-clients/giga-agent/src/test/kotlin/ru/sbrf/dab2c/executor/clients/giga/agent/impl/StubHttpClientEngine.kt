package ru.sbrf.dab2c.executor.clients.giga.agent.impl

import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI

/**
 * Engine answering every request with `200 OK` and the JSON body produced by the handler.
 * Declares timeout capability so that `HttpTimeout` attaches its configuration to the request
 * instead of enforcing socket deadlines itself.
 */
class StubHttpClientEngine(
    private val handler: suspend (HttpRequestData) -> String
) : HttpClientEngineBase("stub") {

    override val config: HttpClientEngineConfig = HttpClientEngineConfig()

    override val supportedCapabilities = setOf(HttpTimeoutCapability)

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val body = handler(data)
        return HttpResponseData(
            statusCode = HttpStatusCode.OK,
            requestTime = GMTDate(),
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            version = HttpProtocolVersion.HTTP_1_1,
            body = ByteReadChannel(body),
            callContext = callContext()
        )
    }
}
