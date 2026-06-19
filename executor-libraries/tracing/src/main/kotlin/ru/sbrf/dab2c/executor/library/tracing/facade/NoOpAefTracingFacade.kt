package ru.sbrf.dab2c.executor.library.tracing.facade

import io.opentelemetry.api.trace.Span

/** Fallback facade used when AEF voice tracing is disabled. All operations are no-ops. */
class NoOpAefTracingFacade : AefTracingFacade {

    override suspend fun startDownstreamGrpcOutputRequest(
        spanName: String,
        path: String,
        settings: Any
    ): Span = Span.getInvalid()

    override fun endDownstreamGrpcOutputRequest(span: Span, statusCode: String) = Unit

    override suspend fun startVoiceSession(spanName: String, settings: Any, parent: Span?): Span = Span.getInvalid()

    override fun endVoiceSession(span: Span, settings: Any) = Unit

    override suspend fun startVoiceTurn(
        spanName: String,
        inputJson: String,
        parent: Span?,
    ): Span = Span.getInvalid()

    override fun endVoiceTurn(span: Span, output: VoiceTurnOutput) = Unit

    override suspend fun startVoiceLlmTurn(
        spanName: String,
        inputJson: String,
        parent: Span?,
    ): Span = Span.getInvalid()

    override fun endVoiceLlmTurn(span: Span, output: VoiceLlmTurnOutput) = Unit

    override suspend fun startTool(functionName: String, arguments: String, parent: Span?): Span = Span.getInvalid()

    override fun endTool(span: Span, outputJson: String) = Unit

    override suspend fun startHttpOutputRequest(
        spanName: String,
        method: String,
        path: String,
        requestBody: String,
        requestHeaders: Map<String, List<String>>
    ): Span = Span.getInvalid()

    override fun endHttpOutputRequest(
        span: Span,
        statusCode: Int,
        responseBody: String,
        responseHeaders: Map<String, List<String>>
    ) = Unit
}
