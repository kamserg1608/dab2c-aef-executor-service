package ru.sbrf.dab2c.executor.voice.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.ContentFromModel
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Error
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.InputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.OutputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Warning
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.time.TimeProvider
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.session.observer.ErrorEmitted
import ru.sbrf.dab2c.executor.voice.session.observer.FunctionCallReceived
import ru.sbrf.dab2c.executor.voice.session.observer.FunctionResultSent
import ru.sbrf.dab2c.executor.voice.session.observer.TurnCompleted
import ru.sbrf.dab2c.executor.voice.session.observer.TurnEvent
import ru.sbrf.dab2c.executor.voice.session.observer.VoiceSessionObserver
import ru.sbrf.dab2c.executor.voice.session.observer.VoiceSettings
import ru.sbrf.dab2c.executor.voice.session.observer.WarningEmitted
import java.util.Collections

/**
 * Accumulates dialog transcriptions from the chunk stream and notifies the given [VoiceSessionObserver].
 * Fan-out and exception isolation are the composite observer's responsibility.
 */
@Suppress("TooManyFunctions")
class VoiceSessionObserverDelegate(
    private val delegate: ChunkProcessingService,
    private val observer: VoiceSessionObserver,
    private val timeProvider: TimeProvider
) : ChunkProcessingService {

    private val now: () -> Long = timeProvider::currentTimeMillis

    private var userReplica = ReplicaAccumulator(now)
    private var assistantReplica = ReplicaAccumulator(now)
    private var currentSegment: ReplicaAccumulator? = null

    private var phase = Phase.AWAITING_INPUT
    private var totalTokens: Int = 0
    private val turnEvents: MutableList<TurnEvent> =
        Collections.synchronizedList(mutableListOf())

    override fun processRequestChunks(requestsChunks: Flow<GigaVoiceRequest>): Flow<GigaVoiceRequest> =
        delegate.processRequestChunks(requestsChunks)
            .onEach { request ->
                when (request.requestCase) {
                    GigaVoiceRequest.RequestCase.SETTINGS ->
                        handleSettingsRequest(request.settings)
                    GigaVoiceRequest.RequestCase.INPUT ->
                        if (request.input.hasAudioContent()) handleAudioRequest()
                    GigaVoiceRequest.RequestCase.FUNCTION_RESULT ->
                        handleFunctionResult(request.functionResult)
                    else -> Unit
                }
            }

    override fun processResponseChunks(responsesChunks: Flow<GigaVoiceResponse>): Flow<GigaVoiceResponse> {
        val accumulatedChunks = responsesChunks
            .onStart { emitSessionStart() }
            .onEach { response ->
                when (response.responseCase) {
                    GigaVoiceResponse.ResponseCase.INPUT_TRANSCRIPTION ->
                        handleInputTranscription(response.inputTranscription)
                    GigaVoiceResponse.ResponseCase.OUTPUT_TRANSCRIPTION ->
                        handleOutputTranscription(response.outputTranscription)
                    GigaVoiceResponse.ResponseCase.WARNING ->
                        handleWarning(response.warning)
                    GigaVoiceResponse.ResponseCase.ERROR ->
                        handleError(response.error)
                    GigaVoiceResponse.ResponseCase.FUNCTION_CALL ->
                        handleFunctionCalling(response.functionCall)
                    GigaVoiceResponse.ResponseCase.OUTPUT ->
                        handleOutput(response.output)
                    else -> Unit
                }
            }
            .onCompletion { cause ->
                flushPendingDialogTurn()
                observer.onSessionCompleted(cause)
            }

        return delegate.processResponseChunks(accumulatedChunks)
    }

    private suspend fun handleSettingsRequest(settings: Settings) {
        val json = ObjectMappers.MAPPER.writeValueAsString(settings)
        val settingsMap: Map<String, Any?> = ObjectMappers.MAPPER.readValue(json, SETTINGS_MAP_TYPE)
        observer.onSettingsReceived(VoiceSettings(settingsData = settingsMap))
    }

    private suspend fun handleAudioRequest() {
        if (userReplica.hasStarted) return
        val ts = now()
        userReplica.markStarted(ts)
        observer.onUserReplicaStarted(ts)
    }

    private suspend fun handleFunctionResult(result: FunctionResult) {
        val event = FunctionResultSent(name = result.functionName, content = result.content, atMs = now())
        turnEvents += event
        observer.onFunctionResultSent(event)
    }

    private suspend fun handleFunctionCalling(functionCalling: FunctionCalling) {
        val event = FunctionCallReceived(
            name = functionCalling.functionCall.name,
            arguments = functionCalling.functionCall.arguments,
            atMs = now(),
        )
        turnEvents += event
        observer.onFunctionCallReceived(event)
        closeAssistantSegment()
    }

    private suspend fun handleOutput(content: ContentFromModel) {
        val ts = now()
        assistantReplica.markStarted(ts)
        openAssistantSegmentIfNeeded(ts)
        when (content.responseCase) {
            ContentFromModel.ResponseCase.AUDIO -> {
                if (content.audio.isFinal) {
                    assistantReplica.markEnded(ts)
                    closeAssistantSegment()
                }
            }
            ContentFromModel.ResponseCase.ADDITIONAL_DATA -> {
                if (content.additionalData.hasUsage()) {
                    totalTokens += content.additionalData.usage.totalTokens
                    observer.onUsageUpdated(totalTokens)
                }
            }
            else -> Unit
        }
        if (content.interrupted) {
            observer.onAssistantInterrupted(ts)
            closeAssistantSegment()
        }
    }

    private suspend fun handleInputTranscription(transcription: InputTranscription) {
        val ts = now()
        if (phase == Phase.ACCUMULATING_OUTPUT) {
            emitTurnCompleted()
            reset()
        }
        phase = Phase.ACCUMULATING_INPUT
        userReplica.appendText(transcription.text, ts)
        logger.debug { "Accumulated input chunk: '${transcription.text}'" }
    }

    private suspend fun handleOutputTranscription(transcription: OutputTranscription) {
        val ts = now()
        if (phase != Phase.ACCUMULATING_OUTPUT) {
            emitUserReplicaCompletedIfAny()
        }
        phase = Phase.ACCUMULATING_OUTPUT
        openAssistantSegmentIfNeeded(ts)
        assistantReplica.appendText(transcription.text, ts)
        currentSegment!!.appendText(transcription.text, ts)
        logger.debug { "Accumulated output chunk: '${transcription.text}'" }
    }

    private suspend fun handleWarning(warning: Warning) {
        val event = WarningEmitted(message = warning.message, atMs = now())
        turnEvents += event
        observer.onWarningEmitted(event)
    }

    private suspend fun handleError(error: Error) {
        val event = ErrorEmitted(status = error.status, message = error.message, atMs = now())
        turnEvents += event
        observer.onErrorEmitted(event)
    }

    private suspend fun openAssistantSegmentIfNeeded(atMs: Long) {
        if (currentSegment != null) return
        currentSegment = ReplicaAccumulator(now).also { it.markStarted(atMs) }
        observer.onAssistantReplicaStarted(atMs)
    }

    private suspend fun closeAssistantSegment() {
        val segment = currentSegment ?: return
        segment.markEnded(now())
        val replica = segment.toReplica()
        currentSegment = null
        observer.onAssistantReplicaCompleted(replica)
    }

    private suspend fun emitUserReplicaCompletedIfAny() {
        if (userReplica.isEmpty) return
        observer.onUserReplicaCompleted(userReplica.toReplica())
    }

    private suspend fun flushPendingDialogTurn() {
        if (currentSegment != null) closeAssistantSegment()
        if (userReplica.isEmpty && assistantReplica.isEmpty) return
        logger.debug { "Flushing pending dialog turn on session completion" }
        emitTurnCompleted()
    }

    private suspend fun emitTurnCompleted() {
        val event = TurnCompleted(
            userReplica = userReplica.takeUnless { it.isEmpty }?.toReplica(),
            assistantReplica = assistantReplica.takeUnless { it.isEmpty }?.toReplica(),
            turnEvents = ArrayList(turnEvents),
            totalTokens = totalTokens.takeIf { it > 0 },
        )
        logger.info {
            "Dialog turn completed: input='${event.userReplica?.text.orEmpty()}', " +
                "output='${event.assistantReplica?.text.orEmpty()}'"
        }
        observer.onTurnCompleted(event)
    }

    private suspend fun emitSessionStart() {
        observer.onSessionStarted()
    }

    private fun reset() {
        userReplica = ReplicaAccumulator(now)
        assistantReplica = ReplicaAccumulator(now)
        currentSegment = null
        phase = Phase.AWAITING_INPUT
        totalTokens = 0
        turnEvents.clear()
    }

    private enum class Phase {
        AWAITING_INPUT,
        ACCUMULATING_INPUT,
        ACCUMULATING_OUTPUT
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val SETTINGS_MAP_TYPE = object : TypeReference<Map<String, Any?>>() {}
    }
}
