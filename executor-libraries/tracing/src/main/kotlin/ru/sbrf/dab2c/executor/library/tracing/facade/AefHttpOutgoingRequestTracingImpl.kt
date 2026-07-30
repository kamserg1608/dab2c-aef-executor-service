package ru.sbrf.dab2c.executor.library.tracing.facade

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import ru.sbrf.dab2c.executor.library.common.runCatchingCancellable
import ru.sbrf.dab2c.executor.library.tracing.mapper.ObserverEventMappers
import kotlin.coroutines.cancellation.CancellationException

private val logger = KotlinLogging.logger {}

/**
 * Implementation of [AefHttpOutgoingRequestTracing].
 */
class AefHttpOutgoingRequestTracingImpl(
    private val facade: AefTracingFacade,
    private val objectMapper: ObjectMapper,
) : AefHttpOutgoingRequestTracing {
    override suspend fun <T : Any> trace(
        spanName: String,
        path: String,
        request: Any,
        block: suspend () -> T
    ): T {
        val span: Span? = openSpan(spanName, path, request)
        return try {
            val result = invokeInSpanContext(span, block)
            closeOnSuccess(span, path, result)
            result
        } catch (e: CancellationException) {
            runCatchingCancellable { span?.end() }
                .onFailure { logger.warn(it) { "endHttpOutputRequest cancel $path failed" } }
            throw e
        } catch (e: Exception) {
            closeOnError(span, path, e)
            throw e
        }
    }

    private suspend fun <T : Any> invokeInSpanContext(span: Span?, block: suspend () -> T): T =
        if (span != null && span.spanContext.isValid) {
            withContext(Context.current().with(span).asContextElement()) { block() }
        } else {
            block()
        }

    private suspend fun openSpan(spanName: String, path: String, request: Any): Span? =
        runCatchingCancellable {
            facade.startHttpOutputRequest(
                spanName = spanName,
                method = "POST",
                path = path,
                requestBody = objectMapper.writeValueAsString(request),
                requestHeaders = emptyMap()
            )
        }.onFailure { logger.warn(it) { "startHttpOutputRequest $path failed" } }.getOrNull()

    private fun closeOnSuccess(span: Span?, path: String, result: Any) {
        if (span == null) return
        runCatchingCancellable {
            facade.endHttpOutputRequest(
                span = span,
                statusCode = HTTP_OK,
                responseBody = objectMapper.writeValueAsString(result),
                responseHeaders = emptyMap()
            )
        }.onFailure { logger.warn(it) { "endHttpOutputRequest success $path failed" } }
    }

    private fun closeOnError(span: Span?, path: String, error: Exception) {
        if (span == null) return
        runCatchingCancellable {
            span.setAttribute(
                AefAttributeKeys.ERROR,
                ObserverEventMappers.toVoiceErrorJson(HTTP_INTERNAL_ERROR, error.message.orEmpty())
            )
            facade.endHttpOutputRequest(
                span = span,
                statusCode = HTTP_INTERNAL_ERROR,
                responseBody = "",
                responseHeaders = emptyMap()
            )
        }.onFailure { logger.warn(it) { "endHttpOutputRequest failure $path failed" } }
    }

    private companion object {
        const val HTTP_OK = 200
        const val HTTP_INTERNAL_ERROR = 500
    }
}
