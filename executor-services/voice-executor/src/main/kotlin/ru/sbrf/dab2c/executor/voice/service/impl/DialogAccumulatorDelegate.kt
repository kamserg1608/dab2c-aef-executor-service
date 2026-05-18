package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.ContentFromModel
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Error
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.InputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.OutputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Warning
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogTurnEvent
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogTurnExtra
import ru.sbrf.dab2c.executor.clients.kap.producer.model.ErrorPayload
import ru.sbrf.dab2c.executor.clients.kap.producer.model.FunctionCallDetails
import ru.sbrf.dab2c.executor.clients.kap.producer.model.FunctionCallPayload
import ru.sbrf.dab2c.executor.clients.kap.producer.model.FunctionResultPayload
import ru.sbrf.dab2c.executor.clients.kap.producer.model.WarningPayload
import ru.sbrf.dab2c.executor.library.audit.model.AuditMessageSchema
import ru.sbrf.dab2c.executor.library.audit.model.InteractionAuditRequest
import ru.sbrf.dab2c.executor.library.audit.port.InteractionAuditor
import ru.sbrf.dab2c.executor.library.time.TimeProvider
import ru.sbrf.dab2c.executor.voice.exception.TolerantExceptionRegistry
import ru.sbrf.dab2c.executor.voice.model.currentFeatureToggles
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.service.api.DialogTurnPublisher
import java.util.Collections

/** Per-turn timestamp boundaries for dialog metrics. */
private data class TurnTimestamps(
    var outputStart: Long = 0L,
    var outputEnd: Long = 0L,
    var userMessageStart: Long? = null,
    var userMessageEnd: Long? = null,
    var assistantMessageStart: Long? = null,
    var assistantMessageEnd: Long? = null
)

/**
 * ChunkProcessingService decorator that accumulates dialog transcriptions and publishes them.
 *
 * Accumulates InputTranscription chunks until OutputTranscription arrives,
 * then accumulates OutputTranscription chunks until next InputTranscription.
 * When a complete input-output pair is collected, publishes the dialog turn
 * and emits a DAB2C_EXTERNAL_INTERACTION audit event per turn.
 */
class DialogAccumulatorDelegate(
    private val delegate: ChunkProcessingService,
    private val dialogTurnPublisher: DialogTurnPublisher,
    private val auditor: InteractionAuditor,
    private val timeProvider: TimeProvider
) : ChunkProcessingService {

    private val inputChunks = mutableListOf<String>()
    private val outputChunks = mutableListOf<String>()

    private var phase = Phase.AWAITING_INPUT
    private var timestamps = TurnTimestamps()

    private var totalTokens: Int = 0
    private val turnEvents: MutableList<DialogTurnEvent> =
        Collections.synchronizedList(mutableListOf())

    override fun processRequestChunks(requestsChunks: Flow<GigaVoiceRequest>): Flow<GigaVoiceRequest> =
        delegate.processRequestChunks(requestsChunks).onEach { request ->
            when (request.requestCase) {
                GigaVoiceRequest.RequestCase.INPUT ->
                    if (request.input.hasAudioContent()) handleAudioRequest()
                GigaVoiceRequest.RequestCase.FUNCTION_RESULT ->
                    handleFunctionResult(request.functionResult)
                else -> Unit
            }
        }

    override fun processResponseChunks(responsesChunks: Flow<GigaVoiceResponse>): Flow<GigaVoiceResponse> {
        val accumulatedChunks = responsesChunks
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
                if (cause != null && !TolerantExceptionRegistry.isTolerant(cause)) {
                    sendFailedAudit(cause)
                }
            }

        return delegate.processResponseChunks(accumulatedChunks)
    }

    private fun handleAudioRequest() {
        if (timestamps.userMessageStart == null) {
            timestamps.userMessageStart = timeProvider.currentTimeMillis()
        }
    }

    private fun handleFunctionResult(result: FunctionResult) {
        turnEvents.add(
            DialogTurnEvent(
                eventName = EVENT_FUNCTION_RESULT,
                timestamp = timeProvider.currentTimeMillis(),
                payload = FunctionResultPayload(
                    content = result.content,
                    functionName = result.functionName
                )
            )
        )
    }

    private fun handleFunctionCalling(functionCalling: FunctionCalling) {
        turnEvents.add(
            DialogTurnEvent(
                eventName = EVENT_FUNCTION_CALL,
                timestamp = timeProvider.currentTimeMillis(),
                payload = FunctionCallPayload(
                    functionCall = FunctionCallDetails(
                        name = functionCalling.functionCall.name,
                        arguments = functionCalling.functionCall.arguments
                    ),
                    timestamp = functionCalling.timestamp
                )
            )
        )
    }

    private fun handleOutput(content: ContentFromModel) {
        if (timestamps.assistantMessageStart == null) {
            timestamps.assistantMessageStart = timeProvider.currentTimeMillis()
        }
        when (content.responseCase) {
            ContentFromModel.ResponseCase.AUDIO ->
                if (content.audio.isFinal) {
                    timestamps.assistantMessageEnd = timeProvider.currentTimeMillis()
                }
            ContentFromModel.ResponseCase.ADDITIONAL_DATA ->
                if (content.additionalData.hasUsage()) {
                    totalTokens += content.additionalData.usage.totalTokens
                }
            else -> Unit
        }
    }

    private suspend fun handleInputTranscription(transcription: InputTranscription) {
        val text = transcription.text

        timestamps.userMessageEnd = timeProvider.currentTimeMillis()

        if (phase == Phase.ACCUMULATING_OUTPUT) {
            publishDialogTurn()
            reset()
        }

        phase = Phase.ACCUMULATING_INPUT
        inputChunks.add(text)

        logger.debug { "Accumulated input chunk: '$text'" }
    }

    private fun handleOutputTranscription(transcription: OutputTranscription) {
        if (outputChunks.isEmpty()) {
            timestamps.outputStart = timeProvider.currentTimeMillis()
        }

        phase = Phase.ACCUMULATING_OUTPUT
        outputChunks.add(transcription.text)
        timestamps.outputEnd = timeProvider.currentTimeMillis()

        logger.debug { "Accumulated output chunk: '${transcription.text}'" }
    }

    private fun handleWarning(warning: Warning) {
        turnEvents.add(
            DialogTurnEvent(
                eventName = EVENT_WARNING,
                timestamp = timeProvider.currentTimeMillis(),
                payload = WarningPayload(message = warning.message)
            )
        )
    }

    private fun handleError(error: Error) {
        turnEvents.add(
            DialogTurnEvent(
                eventName = EVENT_ERROR,
                timestamp = timeProvider.currentTimeMillis(),
                payload = ErrorPayload(
                    status = error.status,
                    message = error.message
                )
            )
        )
    }

    private suspend fun flushPendingDialogTurn() {
        if (inputChunks.isNotEmpty() || outputChunks.isNotEmpty()) {
            logger.debug { "Flushing pending dialog turn on session completion" }
            publishDialogTurn()
        }
    }

    private suspend fun publishDialogTurn() {
        val inputPhrase = inputChunks.joinToString("")
        val outputPhrase = outputChunks.joinToString("")
        val assistantResponseTime = timestamps.outputEnd - timestamps.outputStart
        val kapSendExtra = currentFeatureToggles().kapSendExtra
        val extra = if (kapSendExtra) buildExtra() else null
        val totalTokens = if (kapSendExtra) totalTokens else null

        logger.info {
            "Dialog turn completed: input='$inputPhrase', output='$outputPhrase', " +
                "assistantResponseTime=${assistantResponseTime}ms"
        }

        dialogTurnPublisher.publishDialogTurn(
            inputPhrase, outputPhrase, assistantResponseTime, extra, totalTokens
        )

        sendSuccessAudit(rqMessage = inputPhrase, rsMessage = outputPhrase)
    }

    private fun buildExtra(): DialogTurnExtra =
        DialogTurnExtra(
            events = ArrayList(turnEvents),
            userMessageStartTS = timestamps.userMessageStart ?: timeProvider.currentTimeMillis(),
            userMessageEndTS = timestamps.userMessageEnd ?: timeProvider.currentTimeMillis(),
            assistantMessageStartTS = timestamps.assistantMessageStart ?: timeProvider.currentTimeMillis(),
            assistantMessageEndTS = timestamps.assistantMessageEnd ?: timeProvider.currentTimeMillis()
        )

    private suspend fun sendSuccessAudit(rqMessage: String?, rsMessage: String?) {
        auditor.success(
            request = InteractionAuditRequest(
                answerCode = AuditMessageSchema.ANSWER_CODE_OK,
                rqMessage = rqMessage,
                rsMessage = rsMessage
            )
        )
    }

    private suspend fun sendFailedAudit(cause: Throwable) {
        auditor.failed(
            request = InteractionAuditRequest(
                answerCode = AuditMessageSchema.ANSWER_CODE_FAIL,
                errorCode = TolerantExceptionRegistry.extractErrorCode(cause),
                errorTitle = cause.message ?: cause::class.simpleName ?: AuditMessageSchema.DEFAULT_ERROR_TITLE
            )
        )
    }

    private fun reset() {
        inputChunks.clear()
        outputChunks.clear()
        phase = Phase.AWAITING_INPUT
        timestamps = TurnTimestamps()
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

        private const val EVENT_FUNCTION_CALL = "function_call"
        private const val EVENT_FUNCTION_RESULT = "function_result"
        private const val EVENT_WARNING = "warning"
        private const val EVENT_ERROR = "error"
    }
}
