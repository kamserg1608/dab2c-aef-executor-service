@file:Suppress("StringLiteralDuplication")

package ru.sbrf.dab2c.executor.voice.tracing

import io.grpc.Status
import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.currentCoroutineContext
import ru.sbrf.dab2c.executor.library.tracing.TracingParentElement
import ru.sbrf.dab2c.executor.library.tracing.facade.AefTracingFacade
import ru.sbrf.dab2c.executor.library.tracing.facade.VoiceLlmTurnOutput
import ru.sbrf.dab2c.executor.library.tracing.facade.VoiceTurnOutput
import ru.sbrf.dab2c.executor.library.tracing.mapper.ObserverEventMappers
import ru.sbrf.dab2c.executor.voice.exception.TolerantExceptionRegistry
import ru.sbrf.dab2c.executor.voice.session.observer.ErrorEmitted
import ru.sbrf.dab2c.executor.voice.session.observer.FunctionCallReceived
import ru.sbrf.dab2c.executor.voice.session.observer.FunctionResultSent
import ru.sbrf.dab2c.executor.voice.session.observer.TurnCompleted
import ru.sbrf.dab2c.executor.voice.session.observer.VoiceSessionObserver
import ru.sbrf.dab2c.executor.voice.session.observer.VoiceSettings
import ru.sbrf.dab2c.executor.voice.session.observer.WarningEmitted

/**
 * Publishes voice-session spans (`output_request` downstream gRPC, `voice_session`,
 * `voice_turn`, `voice_llm_turn`, `tool`) for one IVR call. One instance per session.
 */
@Suppress("TooManyFunctions")
class DialogTracingPublisher(
    private val facade: AefTracingFacade,
) : VoiceSessionObserver {

    @Volatile
    private var outputRequestSpan: Span? = null

    @Volatile
    private var voiceSessionSpan: Span? = null

    @Volatile
    private var voiceTurnSpan: Span? = null

    @Volatile
    private var voiceLlmTurnSpan: Span? = null

    @Volatile
    private var toolSpan: Span? = null

    @Volatile
    private var settingsPayload: Map<String, Any?> = emptyMap()
    private var lastSessionError: ErrorEmitted? = null

    override suspend fun onSessionStarted() {
        outputRequestSpan = facade.startDownstreamGrpcOutputRequest(
            SPAN_DOWNSTREAM_STREAM, GRPC_PATH_DOWNSTREAM, settingsPayload
        )
        voiceSessionSpan = facade.startVoiceSession(SPAN_VOICE_SESSION, settingsPayload, outputRequestSpan)
    }

    override suspend fun onSettingsReceived(settings: VoiceSettings) {
        settingsPayload = settings.settingsData
    }

    override suspend fun onAssistantReplicaStarted(atMs: Long) {
        ensureLlmTurnOpen()
    }

    override suspend fun onFunctionCallReceived(event: FunctionCallReceived) {
        ensureLlmTurnOpen()
        toolSpan = facade.startTool(
            functionName = event.name,
            arguments = event.arguments,
            parent = voiceLlmTurnSpan
        )
        currentCoroutineContext()[TracingParentElement]?.set(toolSpan!!)
    }

    override suspend fun onFunctionResultSent(event: FunctionResultSent) {
        currentCoroutineContext()[TracingParentElement]?.clear()
        toolSpan?.let { facade.endTool(it, ObserverEventMappers.toolOutput()) }
        toolSpan = null
    }

    override suspend fun onErrorEmitted(event: ErrorEmitted) {
        lastSessionError = event
    }

    override suspend fun onTurnCompleted(event: TurnCompleted) {
        finalizeTurn(event)
    }

    override suspend fun onSessionCompleted(cause: Throwable?) {
        val statusCode = when {
            cause != null && TolerantExceptionRegistry.isTolerant(cause) -> "OK"
            cause != null -> Status.fromThrowable(cause).code.name
            lastSessionError != null -> "ERROR"
            else -> "OK"
        }
        closeDanglingSpans()
        voiceSessionSpan?.let { facade.endVoiceSession(it, settingsPayload) }
        voiceSessionSpan = null
        outputRequestSpan?.let { facade.endDownstreamGrpcOutputRequest(it, statusCode) }
        outputRequestSpan = null
    }

    private suspend fun ensureLlmTurnOpen() {
        if (voiceTurnSpan == null) {
            voiceTurnSpan = facade.startVoiceTurn(SPAN_VOICE_TURN, voiceSessionSpan)
        }
        if (voiceLlmTurnSpan == null) {
            voiceLlmTurnSpan = facade.startVoiceLlmTurn(SPAN_VOICE_LLM_TURN, voiceTurnSpan)
        }
    }

    private suspend fun finalizeTurn(event: TurnCompleted) {
        val hasData = event.userReplica != null || event.assistantReplica != null || event.turnEvents.isNotEmpty()
        if (!hasData && voiceTurnSpan == null && voiceLlmTurnSpan == null) return

        ensureLlmTurnOpen()
        val warning = event.turnEvents.filterIsInstance<WarningEmitted>()
            .joinToString("; ") { it.message }.takeIf { it.isNotEmpty() }
        val error = event.turnEvents.filterIsInstance<ErrorEmitted>().firstOrNull()
            ?.let { ObserverEventMappers.toVoiceErrorJson(it.status, it.message) }

        closeLlmTurn(event, warning, error)
        closeVoiceTurn(event, warning, error)
    }

    private fun closeLlmTurn(event: TurnCompleted, warning: String?, error: String?) {
        val functionCall = event.turnEvents.filterIsInstance<FunctionCallReceived>().firstOrNull()
        val functionResult = event.turnEvents.filterIsInstance<FunctionResultSent>().firstOrNull()
        val usage = event.llmUsage
        voiceLlmTurnSpan?.let {
            facade.endVoiceLlmTurn(
                it,
                VoiceLlmTurnOutput(
                    inputJson = functionCall
                        ?.let { fc -> ObserverEventMappers.llmTurnInputFunctionCall(fc.name, fc.arguments) }
                        ?: ObserverEventMappers.EMPTY_OBJECT,
                    outputJson = functionResult
                        ?.let { fr -> ObserverEventMappers.llmTurnOutputFunctionResult(fr.name, fr.content) }
                        ?: ObserverEventMappers.EMPTY_OBJECT,
                    totalTokens = event.totalTokens?.toLong(),
                    warning = warning,
                    error = error,
                    promptTokens = usage?.promptTokens?.toLong(),
                    completionTokens = usage?.completionTokens?.toLong(),
                    precachedPromptTokens = usage?.precachedPromptTokens?.toLong(),
                    model = usage?.model,
                    finishReason = usage?.finishReason,
                )
            )
        }
        voiceLlmTurnSpan = null
    }

    private fun closeVoiceTurn(event: TurnCompleted, warning: String?, error: String?) {
        voiceTurnSpan?.let {
            facade.endVoiceTurn(
                it,
                VoiceTurnOutput(
                    inputJson = ObserverEventMappers.voiceTurnInput(event.userReplica?.text),
                    outputJson = ObserverEventMappers.voiceTurnOutput(event.assistantReplica?.text),
                    warning = warning,
                    error = error,
                )
            )
        }
        voiceTurnSpan = null
    }

    private suspend fun closeDanglingSpans() {
        currentCoroutineContext()[TracingParentElement]?.clear()
        toolSpan?.let { facade.endTool(it, ObserverEventMappers.toolOutput()) }
        toolSpan = null
        voiceLlmTurnSpan?.let { facade.endVoiceLlmTurn(it, EMPTY_LLM_TURN_OUTPUT) }
        voiceLlmTurnSpan = null
        voiceTurnSpan?.let { facade.endVoiceTurn(it, EMPTY_TURN_OUTPUT) }
        voiceTurnSpan = null
    }

    private companion object {
        const val GRPC_PATH_DOWNSTREAM = "GigaVoiceProtocol.GigaVoiceService/GigaVoice"
        const val SPAN_DOWNSTREAM_STREAM = "downstream gigavoice stream"
        const val SPAN_VOICE_SESSION = "voice session"
        const val SPAN_VOICE_TURN = "voice turn"
        const val SPAN_VOICE_LLM_TURN = "voice llm turn"

        val EMPTY_TURN_OUTPUT = VoiceTurnOutput(
            inputJson = ObserverEventMappers.EMPTY_OBJECT,
            outputJson = ObserverEventMappers.EMPTY_OBJECT,
            warning = null,
            error = null,
        )
        val EMPTY_LLM_TURN_OUTPUT = VoiceLlmTurnOutput(
            inputJson = ObserverEventMappers.EMPTY_OBJECT,
            outputJson = ObserverEventMappers.EMPTY_OBJECT,
            totalTokens = null,
            warning = null,
            error = null,
        )
    }
}
