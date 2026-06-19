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

    private var outputRequestSpan: Span? = null
    private var voiceSessionSpan: Span? = null
    private var voiceTurnSpan: Span? = null
    private var voiceLlmTurnSpan: Span? = null
    private var toolSpan: Span? = null

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

    override suspend fun onFunctionCallReceived(event: FunctionCallReceived) {
        val inputJson = ObserverEventMappers.llmTurnInputFunctionCall(
            name = event.name,
            argumentsJson = event.arguments,
        )

        ensureLlmTurnOpen(inputJson)

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

    private suspend fun ensureVoiceTurnOpen(
        inputJson: String = ObserverEventMappers.EMPTY_OBJECT,
    ) {
        if (voiceTurnSpan == null) {
            startVoiceTurn(inputJson)
        }
    }

    private suspend fun ensureLlmTurnOpen(
        inputJson: String = ObserverEventMappers.EMPTY_OBJECT,
    ) {
        ensureVoiceTurnOpen(inputJson)

        if (voiceLlmTurnSpan == null) {
            startVoiceLlmTurn(inputJson)
        }
    }

    private suspend fun startVoiceTurn(inputJson: String) {
        voiceTurnSpan = facade.startVoiceTurn(
            spanName = SPAN_VOICE_TURN,
            inputJson = inputJson,
            parent = voiceSessionSpan,
        )
    }

    private suspend fun startVoiceLlmTurn(inputJson: String) {
        voiceLlmTurnSpan = facade.startVoiceLlmTurn(
            spanName = SPAN_VOICE_LLM_TURN,
            inputJson = inputJson,
            parent = voiceTurnSpan,
        )
    }

    private suspend fun finalizeTurn(event: TurnCompleted) {
        val hasData = event.userReplica != null || event.assistantReplica != null || event.turnEvents.isNotEmpty()
        if (!hasData && voiceTurnSpan == null && voiceLlmTurnSpan == null) return

        val warning = warning(event)
        val error = error(event)

        ensureVoiceTurnOpen(ObserverEventMappers.voiceTurnInput(event.userReplica?.text))
        ensureLlmTurnOpen(llmInputJson(event))

        closeLlmTurn(event, warning, error)
        closeVoiceTurn(event, warning, error)
    }

    private fun llmInputJson(event: TurnCompleted): String =
        event.turnEvents
            .filterIsInstance<FunctionCallReceived>()
            .firstOrNull()
            ?.let {
                ObserverEventMappers.llmTurnInputFunctionCall(
                    it.name,
                    it.arguments,
                )
            } ?: ObserverEventMappers.EMPTY_OBJECT

    private fun warning(event: TurnCompleted): String? =
        event.turnEvents
            .filterIsInstance<WarningEmitted>()
            .joinToString("; ") { it.message }
            .takeIf { it.isNotEmpty() }

    private fun error(event: TurnCompleted): String? =
        event.turnEvents
            .filterIsInstance<ErrorEmitted>()
            .firstOrNull()
            ?.let {
                ObserverEventMappers.toVoiceErrorJson(
                    it.status,
                    it.message,
                )
            }

    private fun closeLlmTurn(event: TurnCompleted, warning: String?, error: String?) {
        val functionResult = event.turnEvents.filterIsInstance<FunctionResultSent>().firstOrNull()
        voiceLlmTurnSpan?.let {
            facade.endVoiceLlmTurn(
                it,
                VoiceLlmTurnOutput(
                    outputJson = functionResult
                        ?.let { fr -> ObserverEventMappers.llmTurnOutputFunctionResult(fr.name, fr.content) }
                        ?: ObserverEventMappers.EMPTY_OBJECT,
                    totalTokens = event.totalTokens?.toLong(),
                    warning = warning,
                    error = error,
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
                    outputJson = ObserverEventMappers.voiceTurnOutput(
                        event.assistantReplica?.text,
                    ),
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
            outputJson = ObserverEventMappers.EMPTY_OBJECT,
            warning = null,
            error = null,
        )
        val EMPTY_LLM_TURN_OUTPUT = VoiceLlmTurnOutput(
            outputJson = ObserverEventMappers.EMPTY_OBJECT,
            totalTokens = null,
            warning = null,
            error = null,
        )
    }
}
