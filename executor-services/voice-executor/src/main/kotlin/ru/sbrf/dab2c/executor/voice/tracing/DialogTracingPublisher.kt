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
import ru.sbrf.dab2c.executor.voice.session.observer.ErrorEmitted
import ru.sbrf.dab2c.executor.voice.session.observer.FunctionCallReceived
import ru.sbrf.dab2c.executor.voice.session.observer.FunctionResultSent
import ru.sbrf.dab2c.executor.voice.session.observer.Replica
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
    private var lastUserText: String = ""
    private val pendingWarnings: MutableList<String> = mutableListOf()
    private var pendingTurnError: String? = null
    private var pendingTotalTokens: Long? = null
    private var pendingFunctionCall: FunctionCallReceived? = null
    private var pendingFunctionResult: FunctionResultSent? = null
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

    override suspend fun onUserReplicaCompleted(replica: Replica) {
        lastUserText = replica.text
    }

    override suspend fun onAssistantReplicaStarted(atMs: Long) {
        pendingWarnings.clear()
        pendingTurnError = null
        pendingTotalTokens = null
        pendingFunctionCall = null
        pendingFunctionResult = null
        voiceTurnSpan = facade.startVoiceTurn(SPAN_VOICE_TURN, voiceSessionSpan)
        voiceLlmTurnSpan = facade.startVoiceLlmTurn(SPAN_VOICE_LLM_TURN, voiceTurnSpan)
    }

    override suspend fun onAssistantReplicaCompleted(replica: Replica) {
        closeLlmTurnSpan()
        closeTurnSpan(
            ObserverEventMappers.voiceTurnInput(lastUserText.takeIf { it.isNotEmpty() }),
            ObserverEventMappers.voiceTurnOutput(replica.text.takeIf { it.isNotEmpty() }),
        )
    }

    override suspend fun onFunctionCallReceived(event: FunctionCallReceived) {
        pendingFunctionCall = event
        toolSpan = facade.startTool(
            functionName = event.name,
            arguments = event.arguments,
            parent = voiceLlmTurnSpan
        )
        currentCoroutineContext()[TracingParentElement]?.set(toolSpan!!)
    }

    override suspend fun onFunctionResultSent(event: FunctionResultSent) {
        pendingFunctionResult = event
        currentCoroutineContext()[TracingParentElement]?.clear()
        toolSpan?.let { facade.endTool(it, ObserverEventMappers.toolOutput()) }
        toolSpan = null
    }

    override suspend fun onWarningEmitted(event: WarningEmitted) {
        pendingWarnings += event.message
    }

    override suspend fun onErrorEmitted(event: ErrorEmitted) {
        pendingTurnError = ObserverEventMappers.toVoiceErrorJson(event.status, event.message)
        lastSessionError = event
    }

    override suspend fun onUsageUpdated(totalTokens: Int) {
        pendingTotalTokens = totalTokens.toLong()
    }

    override suspend fun onSessionCompleted(cause: Throwable?) {
        val statusCode = when {
            cause != null -> Status.fromThrowable(cause).code.name
            lastSessionError != null -> "ERROR"
            else -> "OK"
        }
        closeDanglingSegmentSpans()
        voiceSessionSpan?.let { facade.endVoiceSession(it, settingsPayload) }
        voiceSessionSpan = null
        outputRequestSpan?.let { facade.endDownstreamGrpcOutputRequest(it, statusCode) }
        outputRequestSpan = null
    }

    private suspend fun closeDanglingSegmentSpans() {
        currentCoroutineContext()[TracingParentElement]?.clear()
        toolSpan?.let { facade.endTool(it, ObserverEventMappers.toolOutput()) }
        toolSpan = null
        closeLlmTurnSpan()
        closeTurnSpan(
            ObserverEventMappers.voiceTurnInput(lastUserText.takeIf { it.isNotEmpty() }),
            ObserverEventMappers.EMPTY_OBJECT,
        )
    }

    private fun closeLlmTurnSpan() {
        val accumulatedWarning = accumulatedWarning()
        voiceLlmTurnSpan?.let {
            facade.endVoiceLlmTurn(
                it,
                VoiceLlmTurnOutput(
                    inputJson = pendingFunctionCall?.let { fc ->
                        ObserverEventMappers.llmTurnInputFunctionCall(fc.name, fc.arguments)
                    } ?: ObserverEventMappers.EMPTY_OBJECT,
                    outputJson = pendingFunctionResult?.let { fr ->
                        ObserverEventMappers.llmTurnOutputFunctionResult(fr.name, fr.content)
                    } ?: ObserverEventMappers.EMPTY_OBJECT,
                    totalTokens = pendingTotalTokens,
                    warning = accumulatedWarning,
                    error = pendingTurnError,
                )
            )
        }
        voiceLlmTurnSpan = null
    }

    private fun closeTurnSpan(inputJson: String, outputJson: String) {
        voiceTurnSpan?.let {
            facade.endVoiceTurn(
                it,
                VoiceTurnOutput(
                    inputJson = inputJson,
                    outputJson = outputJson,
                    warning = accumulatedWarning(),
                    error = pendingTurnError,
                )
            )
        }
        voiceTurnSpan = null
    }

    private fun accumulatedWarning(): String? =
        pendingWarnings.joinToString("; ").takeIf { it.isNotEmpty() }

    private companion object {
        const val GRPC_PATH_DOWNSTREAM = "GigaVoiceProtocol.GigaVoiceService/GigaVoice"
        const val SPAN_DOWNSTREAM_STREAM = "downstream gigavoice stream"
        const val SPAN_VOICE_SESSION = "voice session"
        const val SPAN_VOICE_TURN = "voice turn"
        const val SPAN_VOICE_LLM_TURN = "voice llm turn"
    }
}
