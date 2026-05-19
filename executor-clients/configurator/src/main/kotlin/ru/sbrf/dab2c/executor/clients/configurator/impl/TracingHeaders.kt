package ru.sbrf.dab2c.executor.clients.configurator.impl

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import java.util.UUID

/** Applies tracing headers (x-trace-id, x-request-id) to outgoing HTTP requests. */
suspend fun HttpRequestBuilder.applyTracingHeaders() {
    val headers = currentHeaders()
    header("x-trace-id", headers.getHeader(RequestHeader.X_TRACE_ID))
    header("x-request-id", UUID.randomUUID().toString())
}
