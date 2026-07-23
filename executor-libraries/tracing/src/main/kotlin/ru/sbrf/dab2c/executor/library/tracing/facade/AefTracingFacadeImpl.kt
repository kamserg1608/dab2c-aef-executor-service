package ru.sbrf.dab2c.executor.library.tracing.facade

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import kotlinx.coroutines.currentCoroutineContext
import ru.sbrf.aef.observability.utils.HeadersUtils
import ru.sbrf.aef.voice.model.VoiceLlmTurnOutputAttributes
import ru.sbrf.aef.voice.tracing.VoiceTracing
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.tracing.TracingParentElement

/** Production implementation of [AefTracingFacade] backed by AEF SDK voice tracing API. */
class AefTracingFacadeImpl(private val tracer: Tracer) : AefTracingFacade {

    override suspend fun startDownstreamGrpcOutputRequest(spanName: String, path: String, settings: Any): Span =
        VoiceTracing.startOutputRequest(
            tracer, spanName, GRPC_METHOD, path, settings, resolveParentContext()
        )

    override fun endDownstreamGrpcOutputRequest(span: Span, statusCode: String) {
        VoiceTracing.endOutputRequest(span, statusCode)
    }

    override suspend fun startVoiceSession(spanName: String, settings: Any, parent: Span?): Span {
        val parentContext = parent?.let { Context.current().with(it) } ?: Context.current()
        return VoiceTracing.startVoiceSession(tracer, spanName, settings, parentContext)
    }

    override fun endVoiceSession(span: Span, settings: Any) {
        VoiceTracing.endVoiceSession(span, settings)
    }

    override suspend fun startVoiceTurn(spanName: String, parent: Span?): Span {
        val parentContext = parent?.let { Context.current().with(it) } ?: Context.current()
        return VoiceTracing.startVoiceTurn(tracer, spanName, EMPTY_INPUT, parentContext)
    }

    override fun endVoiceTurn(span: Span, output: VoiceTurnOutput) {
        span.setAttribute(AefAttributeKeys.INPUT, output.inputJson)
        if (!output.warning.isNullOrEmpty()) span.setAttribute(AefAttributeKeys.WARNING, output.warning)
        if (!output.error.isNullOrEmpty()) span.setAttribute(AefAttributeKeys.ERROR, output.error)
        VoiceTracing.endVoiceTurn(span, parseJsonOrWrap(output.outputJson))
    }

    override suspend fun startVoiceLlmTurn(spanName: String, parent: Span?): Span {
        val parentContext = parent?.let { Context.current().with(it) } ?: Context.current()
        return VoiceTracing.startVoiceLlmTurn(tracer, spanName, EMPTY_INPUT, parentContext)
    }

    override fun endVoiceLlmTurn(span: Span, output: VoiceLlmTurnOutput) {
        span.setAttribute(AefAttributeKeys.INPUT, output.inputJson)
        VoiceTracing.endVoiceLlmTurn(
            span,
            VoiceLlmTurnOutputAttributes.builder()
                .output(parseJsonOrWrap(output.outputJson))
                .totalTokens(output.totalTokens?.toInt())
                .warning(output.warning)
                .error(output.error)
                .build()
        )
    }

    override suspend fun startTool(functionName: String, arguments: String, parent: Span?): Span {
        val parentContext = parent?.let { Context.current().with(it) } ?: Context.current()
        val parsedArgs = parseJsonOrWrap(arguments)
        return VoiceTracing.startTool(tracer, functionName, parsedArgs, parentContext)
    }

    override fun endTool(span: Span, outputJson: String) {
        VoiceTracing.endTool(span, parseJsonOrWrap(outputJson))
    }

    override suspend fun startHttpOutputRequest(
        spanName: String,
        method: String,
        path: String,
        requestBody: String,
        requestHeaders: Map<String, List<String>>
    ): Span {
        val filteredHeaders = HeadersUtils.filterSensitive(requestHeaders)
        val parentContext = resolveParentContext()
        val span = VoiceTracing.startOutputRequest(tracer, spanName, method, path, EMPTY_SETTINGS, parentContext)
        if (span.isRecording) {
            span.setAttribute(AefAttributeKeys.REQUEST_BODY, requestBody)
            span.setAttribute(
                AefAttributeKeys.REQUEST_HEADERS,
                ObjectMappers.MAPPER.writeValueAsString(filteredHeaders)
            )
        }
        return span
    }

    override fun endHttpOutputRequest(
        span: Span,
        statusCode: Int,
        responseBody: String,
        responseHeaders: Map<String, List<String>>
    ) {
        val filteredHeaders = HeadersUtils.filterSensitive(responseHeaders)
        val headersJson = ObjectMappers.MAPPER.writeValueAsString(filteredHeaders)
        VoiceTracing.endOutputRequest(span, statusCode.toString(), responseBody, headersJson)
    }

    private suspend fun resolveParentContext(): Context {
        val element = currentCoroutineContext()[TracingParentElement]
        val base = element?.root() ?: Context.current()
        return element?.get()?.let { base.with(it) } ?: base
    }

    private fun parseJsonOrWrap(json: String): Any =
        try {
            ObjectMappers.MAPPER.readValue(json, Map::class.java)
        } catch (_: Exception) {
            json
        }

    private companion object {
        const val GRPC_METHOD = "GRPC_BIDI"
        val EMPTY_SETTINGS: Any = emptyMap<String, Any>()
        val EMPTY_INPUT: Any = emptyMap<String, Any>()
    }
}
