package ru.sbrf.dab2c.executor.library.tracing.facade

import io.opentelemetry.api.trace.Span
import ru.sbrf.dab2c.executor.library.tracing.mapper.ObserverEventMappers

/** Closing payload for a `voice_turn` span — already-encoded JSON for `aef.output`. */
data class VoiceTurnOutput(
    val outputJson: String,
    val warning: String?,
    val error: String?,
)

/** Closing payload for a `voice_llm_turn` span. */
data class VoiceLlmTurnOutput(
    val outputJson: String,
    val totalTokens: Long?,
    val warning: String?,
    val error: String?,
)

/**
 * Thin facade over AEF SDK voice tracing API. Consumers always receive a non-null bean —
 * either a real implementation or a no-op fallback when `aef.tracing.voice-enabled=false`.
 */
@Suppress("ComplexInterface")
interface AefTracingFacade {

    /** Opens an `output_request` span over the downstream GigaVoice gRPC stream. */
    suspend fun startDownstreamGrpcOutputRequest(spanName: String, path: String, settings: Any): Span

    /** Closes the downstream `output_request` span with a status code. */
    fun endDownstreamGrpcOutputRequest(span: Span, statusCode: String)

    /** Opens a `voice_session` span; [parent] overrides the implicit `Context.current()` when set. */
    suspend fun startVoiceSession(spanName: String, settings: Any, parent: Span? = null): Span

    /** Closes a `voice_session` span. */
    fun endVoiceSession(span: Span, settings: Any)

    /** Opens a `voice_turn` span with canonical `aef.input`. */
    suspend fun startVoiceTurn(
        spanName: String,
        inputJson: String = ObserverEventMappers.EMPTY_OBJECT,
        parent: Span? = null,
    ): Span

    /** Closes a `voice_turn` span; SDK writes `aef.output`, optional warning/error are added manually. */
    fun endVoiceTurn(span: Span, output: VoiceTurnOutput)

    /** Opens a `voice_llm_turn` span. */
    suspend fun startVoiceLlmTurn(
        spanName: String,
        inputJson: String = ObserverEventMappers.EMPTY_OBJECT,
        parent: Span? = null,
    ): Span

    /** Closes a `voice_llm_turn` span; writes `aef.input`, `aef.output`, plus optional `aef.llm.total_tokens`. */
    fun endVoiceLlmTurn(span: Span, output: VoiceLlmTurnOutput)

    /** Opens a `tool` span — [functionName] becomes the span name; SDK formats `aef.input`. */
    suspend fun startTool(functionName: String, arguments: String, parent: Span? = null): Span

    /** Closes a `tool` span; writes `aef.output`. */
    fun endTool(span: Span, outputJson: String)

    /** Opens an HTTP `output_request` span with real request body/headers. */
    suspend fun startHttpOutputRequest(
        spanName: String,
        method: String,
        path: String,
        requestBody: String,
        requestHeaders: Map<String, List<String>>
    ): Span

    /** Closes an HTTP `output_request` span with real response body/headers/status code. */
    fun endHttpOutputRequest(
        span: Span,
        statusCode: Int,
        responseBody: String,
        responseHeaders: Map<String, List<String>>
    )
}
